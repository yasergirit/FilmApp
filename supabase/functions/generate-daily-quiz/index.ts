// Supabase Edge Function: generate-daily-quiz
// Her gece 00:00'da çalışarak Gemini ile 10 yeni soru üretir ve daily_questions tablosuna kaydeder.

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const GEMINI_API_KEY = "AIzaSyAF45d8bwv93UfzytMYjHnLfPTH_CFGVY8";
const GEMINI_URL = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${GEMINI_API_KEY}`;

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const PROMPT = `Sen bir film ve dizi uzmanısın. Bana popüler filmler ve diziler hakkında 10 adet çoktan seçmeli soru hazırla.

Kurallar:
- Her soru 4 şıklı olacak (A, B, C, D).
- Sorular Türkçe olacak.
- Sorular farklı filmler ve dizilerden olsun (aynı filmden 2 soru olmasın).
- Kolay, orta ve zor sorular karışık olsun.
- Sorular şunlarla ilgili olabilir: yönetmenler, oyuncular, film replikleri, ödüller, gişe rekorları, film müzikleri, karakterler, yayın tarihleri vb.
- Tüm dünyadan popüler filmler ve diziler kullan (Marvel, DC, Oscar filmleri, Netflix dizileri, klasik filmler, animasyonlar vb).

SADECE aşağıdaki JSON formatında cevap ver, başka hiçbir şey yazma:
[
  {
    "question": "Soru metni?",
    "options": ["A şıkkı", "B şıkkı", "C şıkkı", "D şıkkı"],
    "correctAnswerIndex": 0
  }
]

correctAnswerIndex 0'dan başlar (0=A, 1=B, 2=C, 3=D).
Sadece JSON array döndür, markdown kod bloğu kullanma.`;

serve(async (req: Request) => {
  try {
    // 1. Gemini API'den soru üret
    const geminiResponse = await fetch(GEMINI_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: PROMPT }] }],
      }),
    });

    if (!geminiResponse.ok) {
      const errText = await geminiResponse.text();
      return new Response(JSON.stringify({ error: "Gemini API error", details: errText }), { status: 500 });
    }

    const geminiData = await geminiResponse.json();
    const rawText = geminiData?.candidates?.[0]?.content?.parts?.[0]?.text ?? "";

    // JSON parse (temizle)
    const cleanJson = rawText.replace(/```json/g, "").replace(/```/g, "").trim();
    const questions = JSON.parse(cleanJson);

    if (!Array.isArray(questions) || questions.length === 0) {
      return new Response(JSON.stringify({ error: "No questions generated" }), { status: 500 });
    }

    // 2. Bugünün tarihini al (UTC)
    const today = new Date().toISOString().split("T")[0]; // yyyy-MM-dd

    // 3. Supabase'e kaydet
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY);

    // Bugün zaten soru varsa ekleme (idempotent)
    const { data: existing } = await supabase
      .from("daily_questions")
      .select("id")
      .eq("created_date", today)
      .limit(1);

    if (existing && existing.length > 0) {
      return new Response(JSON.stringify({ message: "Questions already exist for today", date: today }), { status: 200 });
    }

    const rows = questions.map((q: any) => {
      const letters = ["A", "B", "C", "D"];
      return {
        created_date: today,
        question: q.question,
        option_a: q.options[0],
        option_b: q.options[1],
        option_c: q.options[2],
        option_d: q.options[3],
        right_answer: letters[q.correctAnswerIndex] ?? "A",
      };
    });

    const { error } = await supabase.from("daily_questions").insert(rows);

    if (error) {
      return new Response(JSON.stringify({ error: "DB insert failed", details: error.message }), { status: 500 });
    }

    return new Response(
      JSON.stringify({ success: true, date: today, questionsInserted: rows.length }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  } catch (e) {
    return new Response(JSON.stringify({ error: e.message }), { status: 500 });
  }
});

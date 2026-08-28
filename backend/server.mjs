import express from "express";
import cors from "cors";
import { GoogleGenAI } from "@google/genai";

const app = express();
app.use(cors());
app.use(express.json({ limit: "32kb" }));

const port = process.env.PORT || 8080;
const apiKey = process.env.GEMINI_API_KEY;

const schema = {
  type:"object",
  properties:{
    title:{type:"string"},
    date:{type:"string",description:"YYYY-MM-DD"},
    time:{type:"string",description:"HH:mm"},
    reminderOffsetMinutes:{type:"integer"},
    repeatRule:{type:["string","null"]},
    confidence:{type:"number"}
  },
  required:["title","date","time","reminderOffsetMinutes","repeatRule","confidence"]
};

app.get("/health", (_,res)=>res.json({ok:true}));

app.post("/parse-reminder", async (req,res)=>{
  if (!apiKey) return res.status(503).json({error:"AI backend is not configured"});
  const text = String(req.body?.text || "").trim();
  if (!text) return res.status(400).json({error:"text is required"});

  const now = new Date();
  const today = now.toISOString().slice(0,10);
  const ai = new GoogleGenAI({apiKey});
  const prompt = [
    "Türkçe doğal dil hatırlatma ayrıştırıcısısın.",
    "Kullanıcının cümlesinden tek bir hatırlatma çıkar.",
    "Görevin: title, date, time ve hatırlatma bildiriminin etkinlikten kaç dakika önce olacağını bul.",
    "Belirsiz bilgi varsa confidence değerini düşür.",
    "Bugünün tarihi:", today,
    "Kullanıcının metni:", text
  ].join("\n");

  try {
    const response = await ai.models.generateContent({
      model:"gemini-2.5-flash",
      contents:prompt,
      config:{
        responseMimeType:"application/json",
        responseSchema:schema,
        temperature:0.1
      }
    });
    res.json(JSON.parse(response.text));
  } catch (error) {
    console.error(error);
    res.status(502).json({error:"AI parse failed"});
  }
});

app.listen(port,()=>console.log("Bana Söyle backend listening on "+port));

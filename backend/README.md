# Bana Söyle — Gemini Backend

Bu klasör, Android uygulamasından gelen doğal dil hatırlatma metnini Gemini ile yapılandırılmış hatırlatmaya dönüştürecek güvenli backend katmanının yeridir.

## Güvenlik

- Gemini API anahtarı bu repoya yazılmaz.
- Üretimde anahtar sunucu tarafında secret/environment variable olarak tutulur.
- Android uygulaması Gemini anahtarını doğrudan içermez.
- Backend yalnızca gerekli alanları döndürür: title, date, time, reminderOffsetMinutes, repeatRule.
- Ham ses kaydı backend'e gönderilmez; Android tarafındaki konuşma tanıma sonucunun metni gönderilir.

## Beklenen çıktı

```json
{
  "title": "Dişçi randevusu",
  "date": "2026-08-29",
  "time": "11:00",
  "reminderOffsetMinutes": 30,
  "repeatRule": null,
  "confidence": 0.98
}
```

API anahtarını ortam değişkeninden okuyacak şekilde kurulmalıdır: `GEMINI_API_KEY`.

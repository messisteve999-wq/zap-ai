# ZAP_AI 😈 (com.steve.zapai)

## Termux -> GitHub
    cd ~ && unzip -o ZAP_AI.zip && cd ZAP_AI
    git init && git add . && git commit -m "ZAP_AI v1"
    git branch -M main
    git remote add origin https://github.com/messisteve999-wq/ZAP_AI.git
    git push -u origin main
Puis GitHub > Actions > "Build APK" > télécharger l'artefact ZAP_AI-apk.

## Tester le serveur depuis Termux
    curl -X POST https://gemini-server-steve.onrender.com/reply -H "Content-Type: application/json" -d '{"contact":"Test","message":"Coucou","history":[],"persona":"Réponds en une phrase"}'
Si la route ou le champ de réponse diffère, modifier Config.kt / Gemini.kt.

## Installation
1. Installer l'APK, l'ouvrir. 2. Boutons 1 à 4 dans l'ordre. 3. "Démarrer la bulle".
Si "Accès aux notifications" est grisé : Infos de l'appli > menu ⋮ > "Autoriser les paramètres restreints".

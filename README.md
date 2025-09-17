# ChatApp-Client-Server-Messenger-Java-Gradle-
Eine Chat App in Java (Gradle, JavaFX): TCP-Server, CLI-/GUI-Client, Direktchat über UDP, einfache AES-Verschlüsselung. Lernprojekt Netzwerkprogrammierung.

# ChatApp – Client-Server-Messenger (Java/Gradle)

Ein Lernprojekt zur Netzwerkprogrammierung:
- **Server** über TCP (Port 12345)
- **Clients** als **GUI (JavaFX)** oder **CLI**
- Direktchat zwischen Clients über **UDP**
- Nachrichten mit einfacher **AES-Verschlüsselung** (Demo-Key)

> Hinweis: Die Verschlüsselung dient nur zu Lernzwecken (fester Schlüssel) und ist **nicht** produktionsreif.

## Funktionen
- Registrierung & Login
- Online-Liste aktiver Nutzer
- Einladungen zum 1:1-Chat (UDP)
- GUI (JavaFX) **oder** CLI-Client
- Nebenläufigkeit mit Threads

## Tech-Stack
Java 17+, Gradle, JavaFX, Sockets (TCP/UDP)

## Schnellstart

### 1) Bauen
```bash
# Windows
gradlew.bat clean build

# macOS/Linux
./gradlew clean build


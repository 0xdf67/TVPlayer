# TVPlayer

Aplicação IPTV para Android TV, com interface otimizada para navegação por comando remoto.

## Funcionalidades

- Carregamento de playlists M3U a partir de URL
- Reprodução de streams com ExoPlayer (AndroidX Media3)
- Lista de canais com navegação por D-pad
- Player com controlos de reprodução e informação do canal
- TV Guide / EPG com programa atual e seguinte
- Channel switcher overlay ao mudar de canal
- Suporte para EPG via XMLTV (opcional)

## Estrutura

```
app/src/main/java/com/example/tvplayer/
├── data/                 # Modelos e repositórios
├── parser/               # Parser M3U
├── ui/                   # Activities e adapters
└── TvPlayerApplication.kt

app/src/main/res/
├── layout/               # Layouts das screens e overlays
├── drawable/             # Formas, ícones e fundos
└── values/               # Cores, dimensões, strings e temas
```

## Navegação no Android TV

- **DPAD UP / DOWN**: mudar de canal no player
- **DPAD CENTER / ENTER**: mostrar controlos do player
- **MENU / INFO**: abrir TV Guide
- **BACK**: fechar controlos ou voltar

## Configuração

1. Abrir a app
2. Introduzir o URL da playlist M3U
3. (Opcional) Introduzir o URL do EPG/XMLTV
4. Selecionar "Carregar playlist"
5. Escolher um canal para reproduzir

## Designs

Os designs de referência encontram-se nos ficheiros SVG na raiz do repositório:

- `tv-guide-epg.svg`
- `player.svg`
- `channel-switcher-overlay.svg`

A aplicação replica os elementos visuais principais: paleta escura com acento laranja (#F97316), tipografia clara, badges dos canais, barra de progresso e overlays.

## Build no Android Studio

1. Clonar o repositório e abrir a pasta de projeto no Android Studio.
2. Aguardar pela sincronização do Gradle (requer ligação à Internet para descarregar dependências).
3. Se solicitado, instalar o **Android SDK 34**, **Build Tools 34.0.0** e **NDK** (se aplicável).
4. Ligar um dispositivo Android TV / emulador ou configurar um AVD com perfil de TV (API 21+).
5. Selecionar `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

Configuração do projeto:

- **AGP**: 8.2.0
- **Gradle**: 8.2
- **Kotlin**: 1.9.0
- **compileSdk**: 34
- **minSdk**: 21
- **targetSdk**: 34

## Requisitos

- Android 5.0+ (API 21)
- AndroidX Media3
- ExoPlayer
- Leanback (Android TV)

## Notas

- Se o URL do EPG não for fornecido, é gerada uma grelha de programa de exemplo para demonstração.
- A app usa `android:usesCleartextTraffic="true"` para permitir streams HTTP.

# 🌕 Lunar VR — Sistema Operacional & Experiência VR Mobile 3DoF

**Lunar VR** é uma plataforma e ambiente operacional de Realidade Virtual móvel (VR 3DoF) desenvolvido nativamente para Android. Projetado com uma identidade visual original inspirada no espaço sideral, luz lunar e tecnologia minimalista, oferece renderização estéreo (olho esquerdo/direito), rastreamento de cabeça via sensores inerciais com fusão de dados e calibração por recentralização instantânea, interface flutuante 3D, navegador web integrado com suporte a teclado virtual tridimensional, e controle por apontamento de mão (*Hand Tracking*) e seleção por foco contínuo (*Dwell click* de 1 segundo).

---

## ✨ Principais Características

1. **Identidade Visual Original Lunar VR**:
   - Paleta noturna profunda (*Space Black* `#070A12`), detalhes em ciano lunar brilhante (`#4DEEEA` e `#00E5FF`) e elementos translúcidos em relevo.
   - Design minimalista, cantos arredondados e sem imitações de interfaces de terceiros.

2. **Rastreamento 3DoF de Cabeça Robusto com Calibração**:
   - Utiliza `Sensor.TYPE_ROTATION_VECTOR` de alta precisão quando disponível.
   - Detecção automática com múltiplos níveis de contingência:
     - `GAME_ROTATION_VECTOR` (sem necessidade de magnetômetro);
     - Fusão manual de Giroscópio + Acelerômetro;
     - Inclinômetro por Acelerômetro (em aparelhos básicos sem giroscópio).
   - Botão **"Centralizar visão"** (*Recenter*) instantâneo para eliminar drift.
   - Ajustes de inversão de eixos X e Y.

3. **Renderização Estéreo OpenGL ES 2.0 / 3.0**:
   - Renderização real esquerda/direita com FOV ajustável e projeção de perspectiva independente.
   - Simulação de Distância Interpupilar (IPD) e opção de inversão/espelhamento de olhos.
   - Céu estrelado 3D renderizado em tempo real.

4. **Barra Flutuante Lunar**:
   - Relógio de tempo real, nível de bateria, status dos sensores e atalhos rápidos.
   - Totalmente operável no espaço 3D virtual.

5. **Navegador Web Integrado**:
   - Navegador completo via Android WebView embarcado na cena 3D.
   - Página inicial Google (`https://www.google.com`), suporte a carregamento de URLs, botões Voltar, Avançar, Atualizar e Início.
   - Barra de endereço clicável para inserção direta de texto no headset.

6. **Teclado Virtual VR 3D**:
   - Teclado flutuante tridimensional com modos Minúsculo, Maiúsculo (Shift) e Números/Símbolos (123).
   - Suporte a seleção por apontamento do dedo e foco (*dwell-click*), teclas Backspace (DEL), Barra de espaço e Enter.

7. **Hand Tracking & Seleção por Raycast de 1 Segundo**:
   - Pipeline de câmera integrado (CameraX).
   - Suporte arquitetural a MediaPipe Tasks Hand Landmark com fallback para rastreamento óptico heurístico.
   - Raycast saindo da ponta do indicador em direção aos objetos 3D.
   - Feedback de hover com barra de progresso luminosa animada de 1 segundo; ao completar o tempo de apontamento contínuo, a ação de clique é disparada sem necessidade de toques na tela.

8. **Modo Performance e Ampla Compatibilidade**:
   - Modos **Econômico**, **Normal** e **Qualidade** (com seleção automática `AUTO` ao analisar a RAM e hardware do aparelho).
   - Compatível com Android 8.0+ (API 26) até Android 14+ (API 34).

---

## 📦 Estrutura do Projeto

```
com.lunarvr/
├── MainActivity.kt               # Ponto de entrada, tela de boas-vindas e ciclo de vida
├── vr/
│   ├── VRRenderer.kt             # Renderizador estéreo OpenGL ES e orquestrador da cena
│   ├── VRSession.kt              # Gerenciador de ciclo de vida e estado VR
│   ├── HeadTracking.kt           # Rastreamento 3DoF com múltiplos sensores e recalibração
│   ├── StereoCamera.kt           # Câmeras estéreo esquerda/direita e matrizes de projeção
│   └── RecenterManager.kt        # Gerenciamento de centralização de visão
├── ui/
│   ├── LunarBar.kt               # Barra flutuante principal VR
│   ├── AppButton.kt              # Elemento interativo 3D com bounding box e dwell click
│   ├── SettingsPanel.kt          # Painel completo de ajustes de VR, performance e sistema
│   └── VRPanel.kt                # Painel de textura dinâmica 3D (Canvas -> GL Texture)
├── handtracking/
│   ├── HandTracker.kt            # Interface abstrata do rastreador de mãos
│   ├── MediaPipeHandTracker.kt   # Implementação MediaPipe Hand Landmarker
│   ├── HeuristicHandTracker.kt   # Implementação adaptativa de contingência
│   ├── FingerRay.kt              # Cálculo do raio saindo da ponta do dedo indicador
│   ├── HandRenderer.kt           # Renderizador GL das articulações e raio da mão
│   └── InteractionManager.kt     # Detecção de colisão do raio e temporizador de clique (1s)
├── keyboard/
│   ├── VRKeyboard.kt             # Gerenciamento de layouts de teclado 3D
│   ├── VRKey.kt                  # Tecla 3D individual interativa
│   └── TextInputManager.kt       # Buffer e direcionamento de texto
├── browser/
│   ├── BrowserController.kt      # Controle de navegação web e higienização de URLs
│   ├── BrowserView.kt            # Renderização de WebView off-screen para textura GL
│   └── URLBar.kt                 # Controles de navegação e exibição de URL
└── system/
    ├── DeviceCapabilities.kt     # Inspeção de hardware (Giroscópio, RAM, Câmera, etc.)
    ├── PerformanceManager.kt     # Ajustes de FPS, resolução e nível de detalhes
    └── PermissionManager.kt      # Gerenciamento de permissões de Câmera em tempo de execução
```

---

## 🛠 Compilação e Geração do APK

O projeto utiliza **GitHub Actions** para compilar automaticamente os APKs em cada atualização e publicá-los como artefatos prontos para download.

### Workflow do GitHub Actions (`.github/workflows/build.yml`):
1. Faz o checkout do código-fonte.
2. Configura JDK 17 e Android SDK (API 34).
3. Executa testes unitários (`gradle test`).
4. Compila o APK de desenvolvimento (`gradle assembleDebug`) e release (`gradle assembleRelease`).
5. Publica o artefato **`LunarVR-APK`** contendo o arquivo `app-debug.apk`.

### Baixar o APK:
1. Abra a aba **Actions** no repositório GitHub.
2. Selecione a execução mais recente do workflow **Build Lunar VR APK**.
3. Na seção **Artifacts**, baixe o pacote **LunarVR-APK**.

---

## 📱 Dependências Adicionais

O Lunar VR foi projetado para ser **autossuficiente**, não exigindo a instalação obrigatória de nenhum outro aplicativo para as funções de 3DoF, renderização estéreo, teclado virtual ou navegador.

- **Navegador Web**: Não requer Google Chrome ou navegadores externos instalados, utilizando o `Android System WebView` nativo já integrado ao sistema operacional Android.
- **Hand Tracking**: Executado localmente no próprio aparelho. Caso o modelo de visão computacional MediaPipe não esteja disponível ou a câmera não seja concedida, o sistema opera normalmente em modo 3DoF com fallback adaptativo, sem causar encerramentos inesperados (*crashes*).

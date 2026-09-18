# 🌕 Lunar VR — Sistema Operacional & Experiência VR Mobile 3DoF

**Lunar VR** é uma plataforma e ambiente operacional de Realidade Virtual móvel (VR 3DoF) desenvolvido nativamente para Android. Projetado com uma identidade visual original inspirada no espaço sideral, luz lunar e tecnologia minimalista, oferece renderização estéreo (olho esquerdo/direito), rastreamento de cabeça via sensores inerciais com fusão de dados e calibração por recentralização instantânea, interface flutuante 3D, navegador web integrado com suporte a teclado virtual tridimensional, e controle por foco central (*Gaze Aiming* com mira reticular fixa e seleção por foco contínuo *Dwell click* de 1 segundo).

---

## ✨ Principais Características

1. **Identidade Visual Original Lunar VR**:
   - Paleta noturna profunda (*Space Black* `#070A12`), detalhes em ciano lunar brilhante (`#4DEEEA` e `#00E5FF`) e elementos translúcidos em relevo.
   - Design minimalista, cantos arredondados e sem imitações de interfaces de terceiros.

2. **Rastreamento 3DoF de Cabeça Robusto com Calibração**:
   - Utiliza `Sensor.TYPE_GAME_ROTATION_VECTOR` e `ROTATION_VECTOR` de alta precisão.
   - Detecção automática com múltiplos níveis de contingência:
     - `GAME_ROTATION_VECTOR` (estável, sem saltos magnéticos);
     - Fusão manual de Giroscópio + Acelerômetro;
     - Inclinômetro por Acelerômetro (em aparelhos básicos sem giroscópio).
   - Botão **"Centralizar visão"** (*Recenter*) instantâneo para calibrar o centro para onde você estiver olhando.
   - Ajustes de inversão de eixos X e Y.

3. **Renderização Estéreo OpenGL ES 2.0 / 3.0**:
   - Renderização real esquerda/direita com FOV ajustável e projeção de perspectiva independente.
   - Simulação de Distância Interpupilar (IPD) e opção de inversão/espelhamento de olhos.
   - Céu estrelado 3D renderizado em tempo real.

4. **Barra Flutuante Lunar**:
   - Relógio de tempo real, nível de bateria, status dos sensores e atalhos rápidos.
   - Posicionada na altura natural de repouso dos olhos (`y = -0.25f, z = -1.35f`), não ficando no chão nem obstruindo a visão.

5. **Interação por Retícula Central (Gaze Pointer / Dwell Click 1s)**:
   - Mira reticular ciano sutil fixada no centro do campo de visão.
   - Basta olhar para qualquer botão ou tecla por aproximadamente 1 segundo: a barra de progresso ciano se preenche e o clique é acionado automaticamente, sem toques físicos na tela.

6. **Navegador Web Integrado**:
   - Navegador completo via Android WebView embarcado na cena 3D.
   - Página inicial Google (`https://www.google.com`), suporte a carregamento de URLs, botões Voltar, Avançar, Atualizar e Início.
   - Barra de endereço clicável para inserção direta de texto no headset.

7. **Teclado Virtual VR 3D**:
   - Teclado flutuante tridimensional com modos Minúsculo, Maiúsculo (Shift) e Números/Símbolos (123).
   - Suporte a seleção por foco (*dwell-click*), teclas Backspace (DEL), Barra de espaço e Enter.

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
├── keyboard/
│   ├── VRKeyboard.kt             # Gerenciamento de layouts de teclado 3D
│   ├── VRKey.kt                  # Tecla 3D individual interativa
│   └── TextInputManager.kt       # Buffer e direcionamento de texto
├── browser/
│   ├── BrowserController.kt      # Controle de navegação web e higienização de URLs
│   ├── BrowserView.kt            # Renderização de WebView off-screen para textura GL
│   └── URLBar.kt                 # Controles de navegação e exibição de URL
└── system/
    ├── DeviceCapabilities.kt     # Inspeção de hardware (Giroscópio, RAM, etc.)
    ├── PerformanceManager.kt     # Ajustes de FPS, resolução e nível de detalhes
    └── PermissionManager.kt      # Gerenciamento de permissões
```

---

## 📱 Dependências Adicionais

O Lunar VR é **100% autossuficiente**, não exigindo a instalação obrigatória de nenhum outro aplicativo para as funções de 3DoF, renderização estéreo, teclado virtual ou navegador web.

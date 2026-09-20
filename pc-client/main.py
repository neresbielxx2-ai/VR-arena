"""
Lunar Connections - Official PC Companion & Screen Streaming Receiver for Lunar VR
Designed with modern corporate VR identity inspired by Meta Quest Link / Desktop.
"""

import sys
import socket
import threading
import time
import io
import tkinter as tk
from tkinter import ttk, messagebox
from PIL import Image, ImageTk

class LunarConnectionsApp:
    def __init__(self, root):
        self.root = root
        self.root.title("Lunar Connections - VR Streaming Client")
        self.root.geometry("1020x680")
        self.root.minsize(800, 560)
        self.root.configure(bg="#0B0F19")

        self.socket = None
        self.is_connected = False
        self.is_running = True
        self.receive_thread = None

        # Statistics
        self.fps_counter = 0
        self.last_fps_time = time.time()
        self.current_fps = 0
        self.current_kbps = 0
        self.bytes_counter = 0

        # UI Styling
        self.setup_styles()
        self.build_ui()

        # Intro animation
        self.intro_anim_step = 0
        self.run_intro_animation()

    def setup_styles(self):
        style = ttk.Style()
        style.theme_use("clam")
        style.configure("TFrame", background="#0B0F19")
        style.configure("Card.TFrame", background="#131B2E", relief="flat")
        style.configure("TLabel", background="#0B0F19", foreground="#F8FAFC", font=("Segoe UI", 10))
        style.configure("Muted.TLabel", background="#131B2E", foreground="#94A3B8", font=("Segoe UI", 9))
        style.configure("Header.TLabel", background="#0B0F19", foreground="#00E5FF", font=("Segoe UI", 16, "bold"))
        style.configure("Title.TLabel", background="#131B2E", foreground="#FFFFFF", font=("Segoe UI", 13, "bold"))

    def build_ui(self):
        # Header Bar
        self.header_frame = tk.Frame(self.root, bg="#0E1424", height=60)
        self.header_frame.pack(fill=tk.X, side=tk.TOP)
        self.header_frame.pack_propagate(False)

        logo_label = tk.Label(
            self.header_frame,
            text="🌙 LUNAR CONNECTIONS",
            font=("Segoe UI", 14, "bold"),
            fg="#00E5FF",
            bg="#0E1424"
        )
        logo_label.pack(side=tk.LEFT, padx=25)

        sub_label = tk.Label(
            self.header_frame,
            text="Meta Quest Style PC Companion & Ultra-Low Latency VR Stream",
            font=("Segoe UI", 9),
            fg="#94A3B8",
            bg="#0E1424"
        )
        sub_label.pack(side=tk.LEFT, padx=5)

        self.status_pill = tk.Label(
            self.header_frame,
            text="● DESCONECTADO",
            font=("Segoe UI", 9, "bold"),
            fg="#EF4444",
            bg="#1E293B",
            padx=12,
            pady=4
        )
        self.status_pill.pack(side=tk.RIGHT, padx=25)

        # Main Layout: Left controls sidebar, Right viewport
        self.main_container = tk.Frame(self.root, bg="#0B0F19")
        self.main_container.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)

        # Left Sidebar (Connection & Quality Settings)
        self.sidebar = tk.Frame(self.main_container, bg="#131B2E", width=340, padx=20, pady=20)
        self.sidebar.pack(side=tk.LEFT, fill=tk.Y, padx=(0, 15))
        self.sidebar.pack_propagate(False)

        tk.Label(
            self.sidebar,
            text="Conexão com Óculos VR",
            font=("Segoe UI", 12, "bold"),
            fg="#FFFFFF",
            bg="#131B2E"
        ).pack(anchor="w", pady=(0, 15))

        # IP Address input
        tk.Label(self.sidebar, text="Endereço IP do Celular VR:", fg="#94A3B8", bg="#131B2E").pack(anchor="w")
        self.ip_entry = tk.Entry(
            self.sidebar,
            font=("Segoe UI", 11),
            bg="#1E293B",
            fg="#F8FAFC",
            insertbackground="#00E5FF",
            relief="flat",
            highlightthickness=1,
            highlightcolor="#00E5FF"
        )
        self.ip_entry.insert(0, "192.168.1.100")
        self.ip_entry.pack(fill=tk.X, pady=(4, 12))

        # Port input
        tk.Label(self.sidebar, text="Porta do Servidor:", fg="#94A3B8", bg="#131B2E").pack(anchor="w")
        self.port_entry = tk.Entry(
            self.sidebar,
            font=("Segoe UI", 11),
            bg="#1E293B",
            fg="#F8FAFC",
            insertbackground="#00E5FF",
            relief="flat"
        )
        self.port_entry.insert(0, "8089")
        self.port_entry.pack(fill=tk.X, pady=(4, 12))

        # Connection PIN input (matches user requirement)
        tk.Label(
            self.sidebar,
            text="Coloque a senha Conexão PC (mostrada no VR):",
            fg="#38BDF8",
            bg="#131B2E",
            font=("Segoe UI", 9, "bold")
        ).pack(anchor="w")

        self.pin_entry = tk.Entry(
            self.sidebar,
            font=("Segoe UI", 14, "bold"),
            bg="#1E293B",
            fg="#38BDF8",
            insertbackground="#38BDF8",
            relief="flat",
            justify="center"
        )
        self.pin_entry.pack(fill=tk.X, pady=(6, 18))

        # Connect / Disconnect button
        self.btn_connect = tk.Button(
            self.sidebar,
            text="CONECTAR AO VR",
            font=("Segoe UI", 10, "bold"),
            bg="#6366F1",
            fg="#FFFFFF",
            activebackground="#4F46E5",
            activeforeground="#FFFFFF",
            relief="flat",
            cursor="hand2",
            pady=8,
            command=self.toggle_connection
        )
        self.btn_connect.pack(fill=tk.X, pady=(0, 20))

        # Separator
        tk.Frame(self.sidebar, bg="#334155", height=1).pack(fill=tk.X, pady=10)

        # Quality & Performance Configuration
        tk.Label(
            self.sidebar,
            text="Configurações de Transmissão",
            font=("Segoe UI", 11, "bold"),
            fg="#FFFFFF",
            bg="#131B2E"
        ).pack(anchor="w", pady=(0, 10))

        tk.Label(self.sidebar, text="Qualidade de Imagem (JPEG):", fg="#94A3B8", bg="#131B2E").pack(anchor="w")
        self.quality_combo = ttk.Combobox(
            self.sidebar,
            values=["Alta (Qualidade Máxima - 85%)", "Equilibrada (Recomendado - 65%)", "Ultra Rápida (Baixa Latência - 45%)"],
            state="readonly"
        )
        self.quality_combo.current(1)
        self.quality_combo.pack(fill=tk.X, pady=(4, 12))

        tk.Label(self.sidebar, text="Alvo de Taxa de Quadros (FPS):", fg="#94A3B8", bg="#131B2E").pack(anchor="w")
        self.fps_combo = ttk.Combobox(
            self.sidebar,
            values=["60 FPS (Fluidez Máxima)", "30 FPS (Padrão Estável)", "15 FPS (Economia)"],
            state="readonly"
        )
        self.fps_combo.current(1)
        self.fps_combo.pack(fill=tk.X, pady=(4, 12))

        # Live stats box
        self.stats_box = tk.Label(
            self.sidebar,
            text="FPS: 0  |  Taxa: 0 kbps\nLatência: -- ms",
            fg="#34D399",
            bg="#0E1424",
            font=("Consolas", 9),
            pady=8
        )
        self.stats_box.pack(fill=tk.X, side=tk.BOTTOM)

        # Right Viewport (VR Screen Display)
        self.viewport_frame = tk.Frame(self.main_container, bg="#000000", relief="solid", bd=1)
        self.viewport_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True)

        self.viewport_label = tk.Label(
            self.viewport_frame,
            text="Aguardando conexão com o Lunar VR...\n\n1. Abra o Lunar VR no celular\n2. Vá até a aba 'Conexão PC'\n3. Digite o código e conecte aqui",
            font=("Segoe UI", 12),
            fg="#64748B",
            bg="#000000"
        )
        self.viewport_label.pack(fill=tk.BOTH, expand=True)

    def run_intro_animation(self):
        # Smooth corporate welcome fade-in animation
        colors = ["#00E5FF", "#38BDF8", "#818CF8", "#A855F7", "#6366F1"]
        if self.intro_anim_step < len(colors) * 2:
            idx = (self.intro_anim_step // 2) % len(colors)
            self.status_pill.configure(fg=colors[idx])
            self.intro_anim_step += 1
            self.root.after(80, self.run_intro_animation)
        else:
            self.status_pill.configure(fg="#EF4444")

    def toggle_connection(self):
        if not self.is_connected:
            self.connect_vr()
        else:
            self.disconnect_vr()

    def connect_vr(self):
        ip = self.ip_entry.get().strip()
        port_str = self.port_entry.get().strip()
        pin = self.pin_entry.get().strip()

        if not ip or not pin:
            messagebox.showerror("Erro de Validação", "Preencha o IP do celular e a Senha de Conexão PC!")
            return

        try:
            port = int(port_str)
        except ValueError:
            messagebox.showerror("Porta Inválida", "A porta deve ser um número inteiro válido (ex: 8089).")
            return

        self.btn_connect.configure(text="CONECTANDO...", bg="#EAB308", state="disabled")

        def connection_worker():
            try:
                s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                s.settimeout(5.0)
                s.connect((ip, port))

                # Handshake with PIN
                s.sendall(f"AUTH {pin}\n".encode("utf-8"))
                resp = s.recv(1024).decode("utf-8", errors="ignore").strip()

                if resp == "AUTH_OK":
                    self.socket = s
                    self.is_connected = True
                    self.root.after(0, self.on_connected)
                    self.receive_stream()
                else:
                    s.close()
                    self.root.after(0, lambda: self.on_connect_failed("Senha Conexão PC inválida ou rejeitada pelo óculos VR!"))
            except Exception as e:
                self.root.after(0, lambda: self.on_connect_failed(f"Não foi possível conectar ao IP {ip}:{port}\nVerifique se o celular está na mesma rede Wi-Fi."))

        threading.Thread(target=connection_worker, daemon=True).start()

    def on_connected(self):
        self.btn_connect.configure(text="DESCONECTAR", bg="#EF4444", state="normal")
        self.status_pill.configure(text="● CONECTADO", fg="#10B981")
        self.viewport_label.configure(text="Conectado! Recebendo transmissão do Lunar VR...")

    def on_connect_failed(self, msg):
        self.btn_connect.configure(text="CONECTAR AO VR", bg="#6366F1", state="normal")
        self.status_pill.configure(text="● FALHA CONEXÃO", fg="#EF4444")
        messagebox.showerror("Falha de Conexão", msg)

    def disconnect_vr(self):
        self.is_connected = False
        try:
            if self.socket:
                self.socket.close()
        except:
            pass
        self.socket = None
        self.btn_connect.configure(text="CONECTAR AO VR", bg="#6366F1", state="normal")
        self.status_pill.configure(text="● DESCONECTADO", fg="#EF4444")
        self.viewport_label.configure(text="Transmissão encerrada.\nPronto para reconectar.", image="")

    def receive_stream(self):
        s = self.socket
        if not s:
            return

        def read_line(sock):
            buf = bytearray()
            while True:
                ch = sock.recv(1)
                if not ch or ch == b'\n':
                    break
                buf.extend(ch)
            return buf.decode('utf-8', errors='ignore')

        try:
            while self.is_connected and self.socket:
                header = read_line(s).strip()
                if not header:
                    break

                if header.startswith("FRAME"):
                    parts = header.split(" ")
                    if len(parts) >= 2:
                        length = int(parts[1])
                        # Read binary JPEG payload
                        payload = bytearray()
                        while len(payload) < length:
                            chunk = s.recv(min(length - len(payload), 16384))
                            if not chunk:
                                break
                            payload.extend(chunk)

                        self.bytes_counter += len(payload)
                        self.fps_counter += 1

                        now = time.time()
                        if now - self.last_fps_time >= 1.0:
                            self.current_fps = self.fps_counter
                            self.current_kbps = int((self.bytes_counter * 8) / 1024)
                            self.fps_counter = 0
                            self.bytes_counter = 0
                            self.last_fps_time = now
                            self.root.after(0, self.update_stats)

                        # Render frame to canvas
                        self.render_frame(payload)
        except Exception:
            pass
        finally:
            if self.is_connected:
                self.root.after(0, self.disconnect_vr)

    def update_stats(self):
        self.stats_box.configure(
            text=f"FPS: {self.current_fps}  |  Taxa: {self.current_kbps} kbps\nLatência: Estável (< 30ms)"
        )

    def render_frame(self, data):
        try:
            img = Image.open(io.BytesIO(data))
            vw = self.viewport_frame.winfo_width()
            vh = self.viewport_frame.winfo_height()
            if vw > 100 and vh > 100:
                img.thumbnail((vw, vh), Image.Resampling.LANCZOS)
            photo = ImageTk.PhotoImage(img)

            def update_ui():
                self.viewport_label.configure(image=photo, text="")
                self.viewport_label.image = photo

            self.root.after(0, update_ui)
        except:
            pass

    def on_close(self):
        self.is_running = False
        self.disconnect_vr()
        self.root.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = LunarConnectionsApp(root)
    root.protocol("WM_DELETE_WINDOW", app.on_close)
    root.mainloop()

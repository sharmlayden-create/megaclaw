<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Megaclaw - Siri Style Interface</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { 
            font-family: "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", -apple-system, sans-serif; 
            display: flex; 
            height: 100vh; 
            background-color: #fff8f8; 
            color: #111; 
            overflow: hidden;
        }
        
        /* 核心背景：柔和的红色系高斯模糊 */
        .main-content { 
            flex: 1; 
            background: radial-gradient(circle at 50% 50%, rgba(255, 180, 180, 0.3) 0%, rgba(255, 255, 255, 1) 100%);
            display: flex; 
            flex-direction: column; 
            position: relative;
        }

        /* 顶部区域 */
        .top-bar {
            height: 60px;
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 10;
        }

        /* 对话展示区 */
        .conversation-area {
            flex: 1;
            padding: 20px 40px;
            display: flex;
            flex-direction: column;
            overflow-y: auto;
            gap: 20px;
            scrollbar-width: none;
        }
        .conversation-area::-webkit-scrollbar { display: none; }

        /* 初始欢迎状态 */
        .welcome-hero {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            margin-top: 40px;
            margin-bottom: 20px;
            transition: all 0.5s ease;
        }

        .robot-img {
            width: 100px;
            height: 100px;
            object-fit: contain;
            animation: breathing 4s ease-in-out infinite;
            filter: drop-shadow(0 10px 25px rgba(255, 0, 0, 0.2));
            margin-bottom: 16px;
        }

        @keyframes breathing {
            0%, 100% { transform: scale(1); opacity: 0.9; }
            50% { transform: scale(1.1); opacity: 1; }
        }

        /* 消息气泡通用样式 */
        .message {
            max-width: 70%;
            padding: 12px 20px;
            border-radius: 22px;
            font-size: 16px;
            line-height: 1.6;
            word-wrap: break-word;
            animation: bubbleIn 0.3s cubic-bezier(0.1, 0.9, 0.2, 1);
            position: relative;
        }

        @keyframes bubbleIn {
            from { opacity: 0; transform: translateY(10px) scale(0.95); }
            to { opacity: 1; transform: translateY(0) scale(1); }
        }

        /* 用户消息气泡 - 右侧 */
        .user-message {
            align-self: flex-end;
            background-color: #000;
            color: #fff;
            border-bottom-right-radius: 4px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }

        /* AI 消息气泡 - 左侧 */
        .ai-message {
            align-self: flex-start;
            background: rgba(255, 255, 255, 0.8);
            backdrop-filter: blur(10px);
            color: #111;
            border-bottom-left-radius: 4px;
            border: 1px solid rgba(255,255,255,0.5);
            box-shadow: 0 4px 15px rgba(255, 0, 0, 0.05);
        }

        /* 底部输入区 */
        .bottom-container {
            width: 100%;
            padding: 0 24px 40px;
            display: flex;
            justify-content: center;
            align-items: center;
        }

        /* 彩色流光输入框 */
        .siri-input-wrapper {
            width: 100%;
            max-width: 800px;
            position: relative;
            background: #fff;
            border-radius: 40px;
            padding: 3px;
            background-image: linear-gradient(to right, #ffcc33, #ff6666, #cc66ff, #66ccff, #99ff99);
            box-shadow: 0 10px 30px rgba(0,0,0,0.08);
            display: flex;
            align-items: center;
        }

        .siri-input-inner {
            background: #fff;
            border-radius: 37px;
            width: 100%;
            height: 60px;
            display: flex;
            align-items: center;
            padding: 0 16px;
            gap: 12px;
        }

        .input-avatar {
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background-image: url('image_3bea62.jpg');
            background-size: cover;
            background-position: center;
            box-shadow: 0 2px 5px rgba(255,0,0,0.2);
            flex-shrink: 0;
        }

        .input-box {
            flex: 1;
            border: none;
            outline: none;
            font-size: 17px;
            color: #333;
            background: transparent;
        }

        .input-box::placeholder { color: #aaa; }

        .send-btn {
            width: 36px;
            height: 36px;
            background-color: #000;
            color: #fff;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            border: none;
            opacity: 0;
            transform: scale(0.5);
            transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            pointer-events: none;
        }

        .send-btn.visible {
            opacity: 1;
            transform: scale(1);
            pointer-events: auto;
        }

        .siri-input-wrapper::after {
            content: '';
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            border-radius: 40px;
            z-index: -1;
            background: inherit;
            filter: blur(15px);
            opacity: 0.4;
            animation: glow 3s linear infinite;
        }

        @keyframes glow {
            0% { opacity: 0.3; }
            50% { opacity: 0.6; }
            100% { opacity: 0.3; }
        }
    </style>
</head>
<body>

    <div class="main-content">
        <div class="top-bar"></div>

        <div class="conversation-area" id="chatList">
            <!-- 初始欢迎状态 -->
            <div class="welcome-hero" id="welcomeHero">
                <img src="image_3bea62.jpg" alt="Megaclaw" class="robot-img">
                <div style="font-size: 20px; font-weight: 500; color: #333;">有什么我可以帮您的？</div>
            </div>
        </div>

        <div class="bottom-container">
            <div class="siri-input-wrapper">
                <div class="siri-input-inner">
                    <div class="input-avatar"></div>
                    <input type="text" class="input-box" placeholder="输入你的问题 ~" id="chatInput" autocomplete="off">
                    <button class="send-btn" id="sendBtn">➤</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        const input = document.getElementById('chatInput');
        const sendBtn = document.getElementById('sendBtn');
        const chatList = document.getElementById('chatList');
        const welcomeHero = document.getElementById('welcomeHero');

        // 控制发送按钮
        input.addEventListener('input', () => {
            if (input.value.trim().length > 0) {
                sendBtn.classList.add('visible');
            } else {
                sendBtn.classList.remove('visible');
            }
        });

        function appendMessage(text, role) {
            // 首次输入隐藏欢迎词
            if (welcomeHero) {
                welcomeHero.style.display = 'none';
            }

            const msgDiv = document.createElement('div');
            msgDiv.className = `message ${role === 'user' ? 'user-message' : 'ai-message'}`;
            msgDiv.textContent = text;
            chatList.appendChild(msgDiv);
            
            // 自动滚动
            chatList.scrollTo({
                top: chatList.scrollHeight,
                behavior: 'smooth'
            });
        }

        function handleSend() {
            const text = input.value.trim();
            if (!text) return;

            // 1. 发送用户消息
            appendMessage(text, 'user');
            input.value = '';
            sendBtn.classList.remove('visible');

            // 2. 模拟 AI 回答
            setTimeout(() => {
                appendMessage("收到您的消息，Megaclaw 正在处理中...", "ai");
            }, 800);
        }

        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') handleSend();
        });

        sendBtn.addEventListener('click', handleSend);
    </script>
</body>
</html>
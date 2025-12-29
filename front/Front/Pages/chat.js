const API_BASE = 'http://localhost:8080/test';

document.addEventListener('DOMContentLoaded', () => {
    const chatBtn = document.getElementById('chatBtn');
    const chatWindow = document.getElementById('chatWindow');
    const closeChat = document.getElementById('closeChat');
    const chatInput = document.querySelector('.chat-footer input');
    const chatBody = document.querySelector('.chat-body');

    if (!chatBtn || !chatWindow || !closeChat || !chatInput || !chatBody) return;

    // Check for pending results from a previous chat interaction (e.g. after redirect)
    const pendingResults = sessionStorage.getItem('chatResults');
    if (pendingResults && window.location.pathname.includes('browse.html')) {
        try {
            const results = JSON.parse(pendingResults);
            renderWallpapers(results);
            // sessionStorage.removeItem('chatResults'); // Optional: Clear after showing
        } catch(e) { console.error('Error parsing results', e); }
    }

    // Toggle Chat
    chatBtn.addEventListener('click', () => {
        const isHidden = getComputedStyle(chatWindow).display === 'none';
        if (isHidden) {
            chatWindow.style.display = 'flex';
            loadChatHistory();
        } else {
            chatWindow.style.display = 'none';
        }
    });

    closeChat.addEventListener('click', () => {
        chatWindow.style.display = 'none';
    });

    // Send Message
    chatInput.addEventListener('keypress', async (e) => {
        if (e.key === 'Enter') {
            const message = chatInput.value.trim();
            if (!message) return;

            chatInput.value = '';
            appendMessage('user', message);

            try {
                const response = await fetch(`${API_BASE}/chat?message=${encodeURIComponent(message)}`);
                if (response.ok) {
                    const data = await response.json();
                    appendMessage('assistant', data.message);

                    if (data.results && data.results.length > 0) {
                        sessionStorage.setItem('chatResults', JSON.stringify(data.results));
                        if (!window.location.pathname.includes('browse.html')) {
                            window.location.href = 'browse.html';
                        } else {
                            renderWallpapers(data.results);
                        }
                    }
                } else {
                    appendMessage('assistant', 'Error: Service unavailable.');
                }
            } catch (error) {
                console.error('Chat Error:', error);
                appendMessage('assistant', 'Error: Network issue.');
            }
        }
    });

    function appendMessage(role, text) {
        const div = document.createElement('div');
        div.textContent = text;
        div.style.marginBottom = '10px';
        div.style.padding = '10px 15px';
        div.style.borderRadius = '15px';
        div.style.maxWidth = '80%';
        div.style.wordWrap = 'break-word';
        div.style.fontSize = '0.9rem';
        div.style.lineHeight = '1.4';

        if (role === 'user') {
            div.style.backgroundColor = '#667eea';
            div.style.color = 'white';
            div.style.alignSelf = 'flex-end';
            div.style.borderBottomRightRadius = '2px';
        } else {
            div.style.backgroundColor = '#f1f5f9';
            div.style.color = '#334155';
            div.style.alignSelf = 'flex-start';
            div.style.borderBottomLeftRadius = '2px';
        }

        chatBody.appendChild(div);
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    async function loadChatHistory() {
        try {
            const response = await fetch(`${API_BASE}/chat/history`);
            if (response.ok) {
                const history = await response.json();
                chatBody.innerHTML = ''; // Clear initial content
                history.forEach(msg => {
                    if (msg.role !== 'system') {
                        appendMessage(msg.role, msg.content);
                    }
                });
            }
        } catch (error) {
            console.error('History Error:', error);
        }
    }

    function renderWallpapers(results) {
        const grid = document.getElementById('wallpaperGrid');
        if (!grid) return;
        
        grid.innerHTML = '';
        results.forEach((item, index) => {
            // Map keys to match browse.js logic (CScrap output)
            const data = {
                id: index,
                title: item.titulo || item.title || 'Untitled',
                thumbnailUrl: item.preview || item.src || item.image || item.url || 'https://img.rocket.new/generatedImages/rocket_gen_img_104abccaa-1764813785194.png',
                fullUrl: item.enlace || item.link || '#',
                category: item.sitio || item.tipo || 'Web',
                resolution: item.resolucion || item.info || '',
                hasVideo: item.hasVideo === 'true'
            };
            
            if (!data.thumbnailUrl) return;
            
            const card = document.createElement('div');
            card.className = 'card hover-lift group cursor-pointer';
            card.dataset.category = data.category.toLowerCase();

            card.innerHTML = `
                <div class="image-container aspect-[3/4] relative overflow-hidden rounded-xl">
                    <img src="${data.thumbnailUrl}" alt="${data.title}" class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110" loading="lazy">
                    <div class="image-overlay flex flex-col justify-between p-4 absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity">
                        <div class="flex justify-between items-start">
                            <span class="badge badge-success text-xs">${data.category}</span>
                            <button class="w-8 h-8 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-white/30 transition-colors">
                                <img src="https://img.rocket.new/generatedImages/rocket_gen_img_1876442e1-1765250551228.png" alt="Favorite" class="w-5 h-5">
                            </button>
                        </div>
                        <div>
                            <h3 class="text-white font-semibold text-lg mb-2">
                                <a href="${data.fullUrl}" target="_blank" class="hover:text-accent transition-colors">${data.title}</a>
                            </h3>
                            <div class="flex items-center justify-between text-white/80 text-sm">
                                <div class="flex items-center gap-3">
                                    <span class="flex items-center gap-1">
                                        <img src="https://img.rocket.new/generatedImages/rocket_gen_img_1c1857cda-1764987400448.png" alt="Downloads" class="w-4 h-4">
                                        -
                                    </span>
                                </div>
                                <span class="text-xs">${data.resolution}</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            
            // Add click event to open preview modal
            card.addEventListener('click', (e) => {
                if (e.target.closest('a') || e.target.closest('button')) return; // Allow link and button clicks
                openPreview(data);
            });

            grid.appendChild(card);
        });
        
        // Update count if element exists
        const countEl = document.getElementById('resultsCount');
        if (countEl) countEl.textContent = results.length;
    }

    function openPreview(data) {
        const modal = document.getElementById('previewModal');
        const previewImg = document.getElementById('previewImage');
        const previewTitle = document.getElementById('previewTitle');
        const previewCreator = document.getElementById('previewCreator');
        const closeModal = document.getElementById('closeModal');
        
        if (!modal || !previewImg) return;
        
        // Reset video if exists
        const existingVideo = modal.querySelector('video');
        if (existingVideo) existingVideo.remove();
        previewImg.classList.remove('hidden');
        
        previewImg.src = data.fullUrl || data.thumbnailUrl;
        if (previewTitle) previewTitle.innerHTML = `<a href="${data.fullUrl}" target="_blank" class="hover:text-accent transition-colors">${data.title}</a>`;
        if (previewCreator) previewCreator.textContent = data.category;
        
        // Configure download button
        const downloadBtn = modal.querySelector('.btn-accent');
        if (downloadBtn) {
            downloadBtn.onclick = () => window.open(data.fullUrl, '_blank');
        }
        
        // Show modal
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        
        // Close handlers
        const close = () => {
            const v = modal.querySelector('video');
            if (v) v.remove();
            previewImg.classList.remove('hidden');
            modal.classList.add('hidden');
            modal.classList.remove('flex');
        };
        
        if (closeModal) closeModal.onclick = close;
        
        // Close on click outside
        modal.onclick = (e) => {
            if (e.target === modal) close();
        };

        // Video preview logic
        if (data.hasVideo) {
            fetchVideoPreview(data.fullUrl, data.category);
        }
    }

    async function fetchVideoPreview(url, site) {
        try {
            const response = await fetch(`/scraper/details?url=${encodeURIComponent(url)}&site=${encodeURIComponent(site)}`);
            if (response.ok) {
                const details = await response.json();
                if (details.videoUrl) {
                    const img = document.getElementById('previewImage');
                    const video = document.createElement('video');
                    
                    video.src = details.videoUrl;
                    video.className = "w-full h-auto rounded-t-xl";
                    video.controls = true;
                    video.autoplay = true;
                    video.loop = true;
                    video.muted = true;
                    
                    img.classList.add('hidden');
                    img.parentElement.insertBefore(video, img);
                }
            }
        } catch (e) {
            console.error("Error loading video preview:", e);
        }
    }
});

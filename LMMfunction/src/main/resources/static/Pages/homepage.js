/**
 * Wallpaper System - Homepage Module
 * Handles dynamic rendering for the Trending section on the Homepage.
 * File: front/Front/Pages/homepage.js
 */
const HomeModule = {
    init() {
        this.cacheDOM();
        this.bindEvents();
        this.fetchTrending();
    },

    cacheDOM() {
        this.dom = {
            grid: document.getElementById('trendingGrid'),
            previewModal: document.getElementById('previewModal'),
            closeModal: document.getElementById('closeModal')
        };
    },

    bindEvents() {
        // Modal Close Events
        this.dom.closeModal?.addEventListener('click', () => this.closeModal());
        this.dom.previewModal?.addEventListener('click', (e) => {
            if (e.target === this.dom.previewModal) this.closeModal();
        });
    },

    async fetchTrending() {
        if (!this.dom.grid) return;

        // Show loading state
        this.dom.grid.innerHTML = `
            <div class="col-span-full flex justify-center py-12">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
            </div>`;

        try {
            // Fetch popular wallpapers
            const response = await fetch('/api/wallpapers/search?query=popular&page=1');
            
            if (!response.ok) throw new Error('Backend not reachable');

            const data = await response.json();

            if (!Array.isArray(data) || data.length === 0) {
                this.dom.grid.innerHTML = `
                    <div class="col-span-full text-center py-12 text-text-secondary">
                        <p>No trending wallpapers found.</p>
                    </div>`;
                return;
            }

            // Limit to top 9
            const top9 = data.slice(0, 9);
            this.render(top9);

        } catch (error) {
            console.error('Error fetching trending wallpapers:', error);
            this.dom.grid.innerHTML = `
                <div class="col-span-full text-center py-12 text-red-500">
                    Unable to load trending content.
                </div>`;
        }
    },

    render(items) {
        this.dom.grid.innerHTML = '';
        
        items.forEach(item => {
            // Parse content similar to browse.js
            let props = {};
            try {
                if (item.htmlContent) {
                    props = typeof item.htmlContent === 'string' ? JSON.parse(item.htmlContent) : item.htmlContent;
                }
            } catch (e) {
                console.warn("Error parsing wallpaper content", e);
            }
            if (!props) props = {};

            const wpData = {
                id: item.id,
                title: props.titulo || 'Untitled',
                thumbnailUrl: props.preview || 'https://img.rocket.new/generatedImages/rocket_gen_img_104abccaa-1764813785194.png',
                fullUrl: item.url || props.enlace || '#',
                category: props.sitio || props.tipo || 'Web',
                resolution: props.resolucion || props.info || '',
                rawContent: item.htmlContent
            };

            this.dom.grid.appendChild(this.createCard(wpData));
        });
    },

    createCard(data) {
        const div = document.createElement('div');
        div.className = 'card hover-lift group cursor-pointer wallpaper-item';
        
        div.innerHTML = `
            <div class="image-container aspect-wallpaper relative overflow-hidden rounded-xl">
                <img src="${data.thumbnailUrl}" 
                    alt="${data.title}" 
                    class="w-full h-full object-cover transition-transform duration-700 group-hover:scale-110"
                    loading="lazy">
                <div class="image-overlay flex flex-col justify-between p-4 absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                    <div class="flex items-start justify-between">
                        <span class="badge badge-success text-xs">${data.category}</span>
                        <button class="w-8 h-8 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-white/30 transition-colors">
                            <img src="https://img.rocket.new/generatedImages/rocket_gen_img_1876442e1-1765250551228.png" alt="Favorite" class="w-5 h-5">
                        </button>
                    </div>
                    <div>
                        <h3 class="text-white font-semibold text-lg mb-2 truncate">
                            ${data.title}
                        </h3>
                        <div class="flex items-center justify-between text-white/80 text-sm">
                            <span class="flex items-center gap-1">
                                <img src="https://img.rocket.new/generatedImages/rocket_gen_img_1c1857cda-1764987400448.png" alt="Downloads" class="w-4 h-4">
                                Popular
                            </span>
                            <span class="text-xs">${data.resolution}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        div.addEventListener('click', () => this.openPreview(data));
        return div;
    },

    openPreview(data) {
        const modal = this.dom.previewModal;
        if (!modal) return;

        const img = document.getElementById('previewImage');
        const titleEl = document.getElementById('previewTitle');
        const creatorEl = document.getElementById('previewCreator');
        
        // Reset modal state
        const existingVideo = modal.querySelector('video');
        if (existingVideo) existingVideo.remove();
        if (img) {
            img.classList.remove('hidden');
            img.src = data.thumbnailUrl;
        }
        
        if (titleEl) titleEl.textContent = data.title;
        if (creatorEl) creatorEl.textContent = data.category;
        
        // Setup Download Button
        const downloadBtn = modal.querySelector('.btn-accent');
        if (downloadBtn) {
            const newBtn = downloadBtn.cloneNode(true);
            downloadBtn.parentNode.replaceChild(newBtn, downloadBtn);
            newBtn.onclick = (e) => {
                e.preventDefault();
                this.trackInteraction(data, 'download');
                if (data.fullUrl && data.fullUrl !== '#') window.open(data.fullUrl, '_blank');
            };
        }
        
        modal.classList.remove('hidden');
        modal.classList.add('flex');

        // Fetch high-res media details
        this.fetchMediaContent(data);
    },

    async fetchMediaContent(data) {
        try {
            const response = await fetch(`/scraper/details?url=${encodeURIComponent(data.fullUrl)}&site=${encodeURIComponent(data.category)}`);
            if (response.ok) {
                const details = await response.json();
                
                if (details.videoUrl) {
                    const img = document.getElementById('previewImage');
                    const video = document.createElement('video');
                    
                    video.src = details.videoUrl;
                    video.className = "block w-auto max-w-full max-h-[60vh] mx-auto rounded-t-xl";
                    video.controls = true;
                    video.autoplay = true;
                    video.loop = true;
                    
                    if (img) {
                        img.classList.add('hidden');
                        img.parentElement.insertBefore(video, img);
                    }
                    this.updateDownloadButton(data, details.videoUrl);

                } else if (details.fullImageUrl) {
                    const img = document.getElementById('previewImage');
                    if (img) img.src = details.fullImageUrl;
                    this.updateDownloadButton(data, details.fullImageUrl);
                }
            }
        } catch (e) {
            console.error("Error loading media details:", e);
        }
    },

    updateDownloadButton(data, directUrl) {
        const downloadBtn = this.dom.previewModal.querySelector('.btn-accent');
        if (downloadBtn) {
            const newBtn = downloadBtn.cloneNode(true);
            downloadBtn.parentNode.replaceChild(newBtn, downloadBtn);
            newBtn.onclick = (e) => {
                e.preventDefault();
                this.trackInteraction(data, 'download');
                window.open(directUrl, '_blank');
            };
        }
    },

    closeModal() {
        const modal = this.dom.previewModal;
        const video = modal.querySelector('video');
        if (video) video.remove();
        document.getElementById('previewImage')?.classList.remove('hidden');
        
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    },

    async trackInteraction(data, type) {
        try {
            await fetch('/api/wallpapers/interact', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    url: data.fullUrl,
                    htmlContent: typeof data.rawContent === 'string' ? data.rawContent : JSON.stringify(data.rawContent || {}),
                    type: type
                })
            });
        } catch (e) {
            console.error("Interaction tracking failed", e);
        }
    }
};

document.addEventListener('DOMContentLoaded', () => HomeModule.init());

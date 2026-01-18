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
        
        this.resetDynamicTheme();
        
        // Reset modal state
        const existingVideo = modal.querySelector('video');
        if (existingVideo) existingVideo.remove();
        if (img) {
            img.classList.remove('hidden');
            img.src = data.thumbnailUrl;
        }
        
        if (titleEl) titleEl.textContent = data.title;
        if (creatorEl) creatorEl.textContent = data.category;
        
        // --- NEW: Reset Palette Container ---
        let paletteContainer = document.getElementById('paletteContainer');
        if (!paletteContainer) {
            paletteContainer = document.createElement('div');
            paletteContainer.id = 'paletteContainer';
            paletteContainer.className = 'flex gap-3 mt-4 mb-2 items-center';
            if (creatorEl) {
                const textContainer = creatorEl.closest('div');
                if (textContainer) textContainer.appendChild(paletteContainer);
            }
        }
        paletteContainer.innerHTML = '';

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
                
                if (details.palette && Array.isArray(details.palette)) {
                    this.renderPalette(details.palette);
                    this.applyDynamicTheme(details.palette);
                }

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

    renderPalette(colors) {
        const container = document.getElementById('paletteContainer');
        if (!container) return;
        
        container.innerHTML = '<span class="text-sm text-white/60 mr-2 font-medium">Palette:</span>';
        
        colors.forEach(color => {
            const dot = document.createElement('div');
            dot.className = 'w-6 h-6 rounded-full cursor-pointer hover:scale-110 transition-transform border border-white/20 shadow-lg';
            dot.style.backgroundColor = color;
            dot.title = `Click to copy: ${color}`;
            dot.onclick = () => {
                navigator.clipboard.writeText(color);
                dot.classList.add('ring-2', 'ring-white');
                setTimeout(() => dot.classList.remove('ring-2', 'ring-white'), 200);
            };
            container.appendChild(dot);
        });
    },

    applyDynamicTheme(palette) {
        if (!palette || palette.length === 0) return;

        const modal = this.dom.previewModal;
        const contentWrapper = modal.querySelector('.modal-content');
        const title = document.getElementById('previewTitle');
        const downloadBtn = modal.querySelector('.btn-accent');
        
        // Búsqueda robusta: Usamos 'rounded-b-xl' que es estructural en tu HTML
        // HTML: <div class="p-6 bg-background-light rounded-b-xl">
        let infoContainer = modal.querySelector('.rounded-b-xl.p-6');
        
        // Fallback: Si no lo encuentra por clase, intentamos navegar desde el título
        if (!infoContainer && title) {
             const flexParent = title.closest('.flex'); // El contenedor flex que envuelve título y botón
             if (flexParent) infoContainer = flexParent.parentElement; // El padre de ese flex es el fondo
        }
        
        const mainColor = palette[0];
        const secondaryColor = palette[1] || '#ffffff';
        const isDark = this.isColorDark(mainColor);
        const textColor = isDark ? 'rgba(255,255,255,0.9)' : 'rgba(0,0,0,0.8)';
        const mutedColor = isDark ? 'rgba(255,255,255,0.7)' : 'rgba(0,0,0,0.6)';

        if (contentWrapper) {
            contentWrapper.style.backgroundColor = mainColor;
            contentWrapper.style.transition = 'background-color 0.5s ease';
        }

        if (infoContainer) {
            infoContainer.classList.remove('bg-background-light');
            infoContainer.classList.add('dynamic-theme-bg');
            infoContainer.style.backgroundColor = mainColor;
            infoContainer.style.transition = 'background-color 0.5s ease, color 0.5s ease';
            
            infoContainer.style.color = textColor;
            
            const creator = document.getElementById('previewCreator');
            if (creator) creator.style.color = mutedColor;
        }

        if (title) title.style.color = textColor;

        if (downloadBtn) {
            downloadBtn.style.backgroundColor = secondaryColor;
            downloadBtn.style.borderColor = secondaryColor;
            downloadBtn.style.color = this.isColorDark(secondaryColor) ? '#ffffff' : '#000000';
        }
    },

    resetDynamicTheme() {
        const modal = this.dom.previewModal;
        const contentWrapper = modal.querySelector('.modal-content');
        const title = document.getElementById('previewTitle');
        const creator = document.getElementById('previewCreator');
        const downloadBtn = modal.querySelector('.btn-accent');

        // Intentamos encontrar el contenedor modificado o el original
        let infoContainer = modal.querySelector('.dynamic-theme-bg') || modal.querySelector('.rounded-b-xl.p-6');
        
        if (!infoContainer && title) {
             const flexParent = title.closest('.flex');
             if (flexParent) infoContainer = flexParent.parentElement;
        }

        if (contentWrapper) {
            contentWrapper.style.backgroundColor = '';
        }

        if (infoContainer) {
            infoContainer.style.backgroundColor = '';
            infoContainer.style.color = '';
            infoContainer.style.transition = '';
            infoContainer.classList.remove('dynamic-theme-bg');
            infoContainer.classList.add('bg-background-light');
        }

        if (title) title.style.color = '';
        if (creator) creator.style.color = '';
        
        if (downloadBtn) {
            downloadBtn.style.backgroundColor = '';
            downloadBtn.style.borderColor = '';
            downloadBtn.style.color = '';
        }
    },

    isColorDark(hexColor) {
        const hex = hexColor.replace('#', '');
        const r = parseInt(hex.substr(0, 2), 16);
        const g = parseInt(hex.substr(2, 2), 16);
        const b = parseInt(hex.substr(4, 2), 16);
        const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000;
        return yiq < 128;
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
        
        this.resetDynamicTheme();
        
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

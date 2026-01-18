/**
 * Wallpaper System - Search Module
 * Handles dynamic rendering, filtering, and interactions.
 * File: front/Front/js/search_results.js
 */
const SearchModule = {
    state: {
        wallpapers: [],
        filters: {},
        viewMode: 'medium', // small, medium, large
        bulkMode: false
    },

    init() {
        this.cacheDOM();
        this.bindEvents();
        
        // Get query from URL (e.g. ?q=anime)
        const urlParams = new URLSearchParams(window.location.search);
        const query = urlParams.get('q') || 'anime';
        
        this.updateSearchUI(query);
        this.fetchWallpapers(query);
    },

    cacheDOM() {
        this.dom = {
            grid: document.getElementById('wallpaperGrid'),
            resultsCount: document.getElementById('resultsCount'),
            showingCount: document.getElementById('showingCount'),
            refineInput: document.getElementById('refineSearchInput'),
            autoComplete: document.getElementById('searchAutoComplete'),
            previewModal: document.getElementById('previewModal'),
            // ... add other elements as needed
        };
    },

    bindEvents() {
        // Grid View Toggles
        ['small', 'medium', 'large'].forEach(size => {
            document.getElementById(`grid${size.charAt(0).toUpperCase() + size.slice(1)}`)
                ?.addEventListener('click', () => this.setGridSize(size));
        });

        // Search Input
        this.dom.refineInput?.addEventListener('input', (e) => this.handleSearchInput(e));

        // Modal Close
        document.getElementById('closeModal')?.addEventListener('click', () => this.closeModal());
        this.dom.previewModal?.addEventListener('click', (e) => {
            if (e.target === this.dom.previewModal) this.closeModal();
        });
    },

    updateSearchUI(query) {
        const display = document.getElementById('searchQuery');
        if (display) display.textContent = `"${query}"`;
        if (this.dom.refineInput) this.dom.refineInput.value = query;
    },

    async fetchWallpapers(query) {
        // Show loading state
        this.dom.grid.innerHTML = `
            <div class="col-span-full flex justify-center py-12">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
            </div>`;

        try {
            // Call your Spring Boot Endpoint defined in ScraperController.java
            // Endpoint is /scraper
            const response = await fetch(`/scraper?q=${encodeURIComponent(query)}`);
            
            if (!response.ok) {
                // Fallback for demo if backend isn't running yet
                console.warn("Backend not reachable, using empty state");
                this.state.wallpapers = [];
                this.render();
                return;
            }

            const data = await response.json();

            // Map Java CScrap structure (Map<String, String>) to JS State
            this.state.wallpapers = data.map((item, index) => ({
                id: index,
                title: item.titulo || 'Untitled',
                // 'preview' exists only for Wallpaper Flare in CScrap.java. 
                // Others use a placeholder because 'enlace' is usually a page URL, not an image URL.
                thumbnailUrl: item.preview || 'https://img.rocket.new/generatedImages/rocket_gen_img_104abccaa-1764813785194.png',
                fullUrl: item.enlace || '#',
                category: item.sitio || item.tipo || 'Web',
                resolution: item.resolucion || item.info || ''
            }));

            this.render();

        } catch (error) {
            console.error('Error fetching wallpapers:', error);
            this.dom.grid.innerHTML = `
                <div class="col-span-full text-center py-12 text-red-500">
                    Error loading results. Please ensure the backend is running.
                </div>`;
        }
    },

    setGridSize(size) {
        this.state.viewMode = size;
        const grid = this.dom.grid;
        
        // Reset classes
        grid.className = 'grid gap-6';
        
        // Apply new classes
        const classes = {
            small: ['grid-cols-2', 'md:grid-cols-3', 'lg:grid-cols-4', 'xl:grid-cols-5'],
            medium: ['grid-cols-1', 'sm:grid-cols-2', 'lg:grid-cols-3', 'xl:grid-cols-4'],
            large: ['grid-cols-1', 'sm:grid-cols-2', 'lg:grid-cols-3']
        };
        
        grid.classList.add(...classes[size]);
        
        // Update active button state visually
        ['Small', 'Medium', 'Large'].forEach(s => {
            const btn = document.getElementById(`grid${s}`);
            if(s.toLowerCase() === size) btn.classList.add('bg-surface');
            else btn.classList.remove('bg-surface');
        });
    },

    createWallpaperCard(data) {
        // Template for a single wallpaper card
        const div = document.createElement('div');
        div.className = 'card hover-lift group cursor-pointer wallpaper-item';
        div.dataset.id = data.id;
        
        div.innerHTML = `
            <div class="image-container aspect-wallpaper relative">
                <img src="${data.thumbnailUrl}" 
                    alt="${data.title}" 
                    class="w-full h-full object-cover"
                    loading="lazy">
                <div class="image-overlay flex flex-col justify-between p-4 absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity">
                    <div class="flex items-start justify-between">
                        <div class="flex items-center gap-2">
                            <span class="badge badge-success text-xs">${data.category}</span>
                        </div>
                        <button class="w-8 h-8 bg-white/20 backdrop-blur-sm rounded-full flex items-center justify-center hover:bg-white/30 transition-colors">
                            <img src="https://img.rocket.new/generatedImages/rocket_gen_img_1876442e1-1765250551228.png" alt="Favorite" class="w-5 h-5">
                        </button>
                    </div>
                    <div>
                        <h3 class="text-white font-semibold text-lg mb-2">
                            <a href="${data.fullUrl}" target="_blank" class="hover:text-accent transition-colors">${data.title}</a>
                        </h3>
                        <div class="flex items-center justify-between text-white/80 text-sm">
                            <span>${data.resolution}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        div.addEventListener('click', () => this.openPreview(data));
        return div;
    },

    render() {
        this.dom.grid.innerHTML = '';
        
        if (this.state.wallpapers.length === 0) {
            this.dom.grid.innerHTML = `
                <div class="col-span-full text-center py-12 text-text-secondary">
                    <p>No wallpapers found. Try adjusting your filters.</p>
                </div>
            `;
            this.dom.resultsCount.textContent = '0';
            this.dom.showingCount.textContent = 'Showing 0 results';
            return;
        }

        this.state.wallpapers.forEach(wp => {
            this.dom.grid.appendChild(this.createWallpaperCard(wp));
        });
        
        this.dom.resultsCount.textContent = this.state.wallpapers.length;
        if (this.dom.showingCount) {
            this.dom.showingCount.textContent = `Showing ${this.state.wallpapers.length} results`;
        }
        // Update stats mock
        if (document.getElementById('statTotal')) document.getElementById('statTotal').textContent = this.state.wallpapers.length;
    },

    openPreview(data) {
        const modal = this.dom.previewModal;
        const img = document.getElementById('previewImage');
        
        this.resetDynamicTheme();
        
        // Resetear estado del modal (quitar video anterior si existe)
        const existingVideo = modal.querySelector('video');
        if (existingVideo) existingVideo.remove();
        img.classList.remove('hidden');
        
        img.src = data.thumbnailUrl;
        
        const titleEl = document.getElementById('previewTitle');
        titleEl.innerHTML = `<a href="${data.fullUrl}" target="_blank" class="hover:text-accent transition-colors">${data.title}</a>`;
        
        // --- NEW: Reset Palette Container ---
        let paletteContainer = document.getElementById('paletteContainer');
        if (!paletteContainer) {
            paletteContainer = document.createElement('div');
            paletteContainer.id = 'paletteContainer';
            paletteContainer.className = 'flex gap-3 mt-4 mb-2 items-center';
            const creatorEl = document.getElementById('previewCreator');
            if (creatorEl && creatorEl.parentNode) creatorEl.parentNode.appendChild(paletteContainer);
        }
        paletteContainer.innerHTML = '';

        // Configurar el botón de descarga si existe en este modal
        const downloadBtn = modal.querySelector('.btn-accent');
        if (downloadBtn) {
            const newBtn = downloadBtn.cloneNode(true);
            downloadBtn.parentNode.replaceChild(newBtn, downloadBtn);
            newBtn.onclick = () => window.open(data.fullUrl, '_blank');
        }
        
        modal.classList.remove('hidden');
        modal.classList.add('flex');
        
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
                    video.muted = true;
                    img.classList.add('hidden');
                    img.parentElement.insertBefore(video, img);
                    this.updateDownloadButton(data, details.videoUrl);
                } else if (details.fullImageUrl) {
                    document.getElementById('previewImage').src = details.fullImageUrl;
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
        container.innerHTML = '<span class="text-sm text-text-secondary mr-2 font-medium">Palette:</span>';
        colors.forEach(color => {
            const dot = document.createElement('div');
            dot.className = 'w-6 h-6 rounded-full cursor-pointer hover:scale-110 transition-transform border border-white/20 shadow-lg';
            dot.style.backgroundColor = color;
            dot.onclick = () => navigator.clipboard.writeText(color);
            container.appendChild(dot);
        });
    },

    applyDynamicTheme(palette) {
        if (!palette || palette.length === 0) return;

        const modal = this.dom.previewModal;
        const contentWrapper = modal.querySelector('.modal-content');
        const infoContainer = modal.querySelector('.bg-background-light') || modal.querySelector('.dynamic-theme-bg');
        const title = document.getElementById('previewTitle');
        const downloadBtn = modal.querySelector('.btn-accent');
        
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
        const infoContainer = modal.querySelector('.dynamic-theme-bg');
        const title = document.getElementById('previewTitle');
        const creator = document.getElementById('previewCreator');
        const downloadBtn = modal.querySelector('.btn-accent');

        if (contentWrapper) {
            contentWrapper.style.backgroundColor = '';
        }

        if (infoContainer) {
            infoContainer.style.backgroundColor = '';
            infoContainer.style.color = '';
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
            newBtn.onclick = () => window.open(directUrl, '_blank');
        }
    },

    closeModal() {
        const modal = this.dom.previewModal;
        const video = modal.querySelector('video');
        if (video) video.remove();
        document.getElementById('previewImage').classList.remove('hidden');
        
        this.resetDynamicTheme();
        
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    },

    handleSearchInput(e) {
        clearTimeout(this.searchTimeout);
        this.searchTimeout = setTimeout(() => {
            const query = e.target.value.trim();
            if (query.length > 2) {
                // Update URL without reload
                const url = new URL(window.location);
                url.searchParams.set('q', query);
                window.history.pushState({}, '', url);
                
                this.updateSearchUI(query);
                this.fetchWallpapers(query);
            }
        }, 600);
    }
};

// Initialize the app
document.addEventListener('DOMContentLoaded', () => SearchModule.init());

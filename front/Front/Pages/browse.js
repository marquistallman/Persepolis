/**
 * Wallpaper System - Browse Module
 * Handles dynamic rendering and interactions for the Browse page.
 * File: front/Front/Pages/browse.js
 */
const BrowseModule = {
    state: {
        wallpapers: [],
        viewMode: 'medium',
        searchTimeout: null
    },

    init() {
        this.cacheDOM();
        this.bindEvents();
        
        // Cargar wallpapers iniciales (por defecto busca algo genérico o popular)
        this.fetchWallpapers('popular');
    },

    cacheDOM() {
        this.dom = {
            grid: document.getElementById('wallpaperGrid'),
            resultsCount: document.getElementById('resultsCount'),
            searchInput: document.getElementById('searchInput'),
            searchSuggestions: document.getElementById('searchSuggestions'),
            previewModal: document.getElementById('previewModal'),
            loadMoreBtn: document.getElementById('loadMoreBtn')
        };
    },

    bindEvents() {
        // Grid View Toggles
        ['small', 'medium', 'large'].forEach(size => {
            document.getElementById(`grid${size.charAt(0).toUpperCase() + size.slice(1)}`)
                ?.addEventListener('click', () => this.setGridSize(size));
        });

        // Search Input con Debounce
        this.dom.searchInput?.addEventListener('input', (e) => this.handleSearchInput(e));

        // Modal Close
        document.getElementById('closeModal')?.addEventListener('click', () => this.closeModal());
        this.dom.previewModal?.addEventListener('click', (e) => {
            if (e.target === this.dom.previewModal) this.closeModal();
        });

        // Load More (Simulado por ahora)
        this.dom.loadMoreBtn?.addEventListener('click', () => {
            const btn = this.dom.loadMoreBtn;
            const originalContent = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span>Loading...</span>';
            setTimeout(() => {
                btn.disabled = false;
                btn.innerHTML = originalContent;
            }, 1000);
        });
    },

    async fetchWallpapers(query) {
        // Loading state
        this.dom.grid.innerHTML = `
            <div class="col-span-full flex justify-center py-12">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
            </div>`;

        try {
            // Conexión al endpoint del backend
            const response = await fetch(`/scraper?q=${encodeURIComponent(query)}`);
            
            if (!response.ok) {
                console.warn("Backend not reachable");
                this.state.wallpapers = [];
                this.render();
                return;
            }

            const data = await response.json();

            // Mapeo de datos (igual que en search_results.js)
            this.state.wallpapers = data.map((item, index) => ({
                id: index,
                title: item.titulo || 'Untitled',
                thumbnailUrl: item.preview || 'https://img.rocket.new/generatedImages/rocket_gen_img_104abccaa-1764813785194.png',
                fullUrl: item.enlace || '#',
                category: item.sitio || item.tipo || 'Web',
                resolution: item.resolucion || item.info || '',
                hasVideo: item.hasVideo === 'true'
            }));

            this.render();

        } catch (error) {
            console.error('Error fetching wallpapers:', error);
            this.dom.grid.innerHTML = `
                <div class="col-span-full text-center py-12 text-red-500">
                    Error loading content. Please ensure the backend is running.
                </div>`;
        }
    },

    createWallpaperCard(data) {
        const div = document.createElement('div');
        div.className = 'card hover-lift group cursor-pointer wallpaper-item';
        div.dataset.category = data.category.toLowerCase();
        
        div.innerHTML = `
            <div class="image-container aspect-wallpaper relative">
                <img src="${data.thumbnailUrl}" 
                    alt="${data.title}" 
                    class="w-full h-full object-cover"
                    loading="lazy">
                <div class="image-overlay flex flex-col justify-between p-4 absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity">
                    <div class="flex items-start justify-between">
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
        
        div.addEventListener('click', () => this.openPreview(data));
        return div;
    },

    render() {
        this.dom.grid.innerHTML = '';
        
        if (this.state.wallpapers.length === 0) {
            this.dom.grid.innerHTML = `
                <div class="col-span-full text-center py-12 text-text-secondary">
                    <p>No wallpapers found.</p>
                </div>`;
            this.dom.resultsCount.textContent = '0';
            return;
        }

        this.state.wallpapers.forEach(wp => {
            this.dom.grid.appendChild(this.createWallpaperCard(wp));
        });
        
        this.dom.resultsCount.textContent = this.state.wallpapers.length.toLocaleString();
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
        
        // Update active button state
        ['Small', 'Medium', 'Large'].forEach(s => {
            const btn = document.getElementById(`grid${s}`);
            const img = btn.querySelector('img');
            if(s.toLowerCase() === size) {
                btn.classList.add('bg-surface');
                // Simple filter simulation for icon color change if needed
            } else {
                btn.classList.remove('bg-surface');
            }
        });
    },

    openPreview(data) {
        const modal = this.dom.previewModal;
        const img = document.getElementById('previewImage');
        
        // Resetear estado del modal (quitar video anterior si existe)
        const existingVideo = modal.querySelector('video');
        if (existingVideo) existingVideo.remove();
        img.classList.remove('hidden');
        img.src = data.fullUrl || data.thumbnailUrl;
        
        const titleEl = document.getElementById('previewTitle');
        titleEl.innerHTML = `<a href="${data.fullUrl}" target="_blank" class="hover:text-accent transition-colors">${data.title}</a>`;
        
        document.getElementById('previewCreator').textContent = data.category;
        
        // Configurar el botón de descarga para abrir el enlace original
        const downloadBtn = modal.querySelector('.btn-accent');
        if (downloadBtn) {
            downloadBtn.onclick = () => window.open(data.fullUrl, '_blank');
        }
        
        modal.classList.remove('hidden');
        modal.classList.add('flex');

        // Si el sitio soporta video, buscar el video preview
        if (data.hasVideo) {
            this.fetchVideoPreview(data.fullUrl, data.category);
        }
    },

    async fetchVideoPreview(url, site) {
        try {
            // Llamada al nuevo endpoint que usa SiteDetailScraper
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
                    video.muted = false; // Opcional: iniciar con sonido
                    
                    img.classList.add('hidden');
                    img.parentElement.insertBefore(video, img);
                }
            }
        } catch (e) {
            console.error("Error cargando preview de video:", e);
        }
    },

    closeModal() {
        // Limpiar video al cerrar para detener el audio
        const modal = this.dom.previewModal;
        const video = modal.querySelector('video');
        if (video) video.remove();
        document.getElementById('previewImage').classList.remove('hidden');
        
        modal.classList.add('hidden');
        modal.classList.remove('flex');
    },

    handleSearchInput(e) {
        clearTimeout(this.state.searchTimeout);
        const value = e.target.value.trim();
        
        if (value.length > 0) {
            this.dom.searchSuggestions?.classList.remove('hidden');
        } else {
            this.dom.searchSuggestions?.classList.add('hidden');
        }

        this.state.searchTimeout = setTimeout(() => {
            if (value.length > 2) {
                this.fetchWallpapers(value);
            }
        }, 600);
    }
};

document.addEventListener('DOMContentLoaded', () => BrowseModule.init());

/**
 * Wallpaper System - Browse Module
 * Handles dynamic rendering and interactions for the Browse page.
 * File: front/Front/Pages/browse.js
 */
const BrowseModule = {
    state: {
        wallpapers: [],
        viewMode: 'medium',
        searchTimeout: null,
        filterColor: null,
        filterOrientation: 'All',
        currentPage: 1
    },

    init() {
        this.cacheDOM();
        this.bindEvents();
        
        // Check for URL parameters (Search from Homepage)
        const urlParams = new URLSearchParams(window.location.search);
        const query = urlParams.get('q');

        if (query) {
            if (this.dom.searchInput) this.dom.searchInput.value = query;
            this.fetchWallpapers(query);
        } else {
            // Cargar wallpapers iniciales (por defecto busca algo genérico o popular)
            this.fetchWallpapers('popular');
        }
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
        this.dom.searchInput?.addEventListener('input', (e) => this.handleSearchInput());

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
            
            this.state.currentPage++;
            this.fetchWallpapers(this.dom.searchInput.value || 'popular', true).then(() => {
                btn.disabled = false;
                btn.innerHTML = originalContent;
            });
        });

        // --- FILTROS ---

        // 1. Reset Filters
        document.getElementById('resetFilters')?.addEventListener('click', () => this.resetFilters());

        // 2. Categorías (Checkboxes)
        const categoriesSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Categories');
        if (categoriesSection) {
            const container = categoriesSection.nextElementSibling;
            container.querySelectorAll('input[type="checkbox"]').forEach(cb => {
                cb.addEventListener('change', () => this.triggerSearch());
            });
        }

        // 3. Paleta de Colores
        const colorSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Color Palette');
        if (colorSection) {
            const container = colorSection.nextElementSibling;
            container.querySelectorAll('button').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    // Toggle visual
                    container.querySelectorAll('button').forEach(b => b.classList.remove('ring-2', 'ring-accent', 'ring-offset-2'));
                    e.target.classList.add('ring-2', 'ring-accent', 'ring-offset-2');
                    
                    // Guardar estado (Extraer "Red" de "Red color filter")
                    const label = e.target.getAttribute('aria-label') || '';
                    this.state.filterColor = label.replace(' color filter', '').replace(' filter', '');
                    this.triggerSearch();
                });
            });
        }

        // 4. Resolución (Radio buttons)
        const resSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Resolution');
        if (resSection) {
            const container = resSection.nextElementSibling;
            container.querySelectorAll('input[type="radio"]').forEach(radio => {
                radio.addEventListener('change', () => this.triggerSearch());
            });
        }

        // 5. Orientación (Botones)
        const orientSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Orientation');
        if (orientSection) {
            const container = orientSection.nextElementSibling;
            const buttons = container.querySelectorAll('button');
            buttons.forEach(btn => {
                btn.addEventListener('click', (e) => {
                    // Toggle visual
                    buttons.forEach(b => {
                        b.classList.remove('border-accent', 'bg-accent', 'text-white');
                        b.classList.add('border-border', 'text-text-secondary');
                    });
                    e.target.classList.remove('border-border', 'text-text-secondary');
                    e.target.classList.add('border-accent', 'bg-accent', 'text-white');

                    this.state.filterOrientation = e.target.textContent.trim();
                    this.triggerSearch();
                });
            });
        }
    },

    // Construye la consulta compuesta y ejecuta la búsqueda
    triggerSearch() {
        const queryParts = [];
        
        // 1. Input de texto
        const searchVal = this.dom.searchInput?.value.trim();
        if (searchVal) queryParts.push(searchVal);

        // 2. Categorías seleccionadas
        const categoriesSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Categories');
        if (categoriesSection) {
            const container = categoriesSection.nextElementSibling;
            container.querySelectorAll('input[type="checkbox"]:checked').forEach(cb => {
                const label = cb.nextElementSibling.textContent.trim();
                if (label !== 'All Categories') {
                    queryParts.push(label);
                }
            });
        }

        // 3. Color
        if (this.state.filterColor && this.state.filterColor !== 'Multicolor' && this.state.filterColor !== 'White') {
             queryParts.push(this.state.filterColor);
        }

        // 4. Resolución
        const resSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Resolution');
        if (resSection) {
            const checked = resSection.nextElementSibling.querySelector('input[type="radio"]:checked');
            if (checked) {
                const label = checked.nextElementSibling.textContent.trim();
                if (label.includes('4K')) queryParts.push('4K');
                else if (label.includes('Full HD')) queryParts.push('1080p');
            }
        }

        // 5. Orientación
        if (this.state.filterOrientation && this.state.filterOrientation !== 'All') {
            queryParts.push(this.state.filterOrientation);
        }

        const finalQuery = queryParts.join(' ');
        console.log("--- DEBUG: Triggering search with query:", finalQuery);
        this.fetchWallpapers(finalQuery || 'popular');
    },

    resetFilters() {
        // Resetear checkboxes
        document.querySelectorAll('input[type="checkbox"]').forEach(cb => cb.checked = false);
        // Marcar "All Categories" si existe
        const allCat = Array.from(document.querySelectorAll('span')).find(s => s.textContent === 'All Categories');
        if (allCat && allCat.previousElementSibling) allCat.previousElementSibling.checked = true;

        // Resetear radios (Resolution)
        const allRes = Array.from(document.querySelectorAll('span')).find(s => s.textContent === 'All Resolutions');
        if (allRes && allRes.previousElementSibling) allRes.previousElementSibling.checked = true;

        // Resetear Colores
        this.state.filterColor = null;
        const colorSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Color Palette');
        if (colorSection) {
            colorSection.nextElementSibling.querySelectorAll('button').forEach(b => b.classList.remove('ring-2', 'ring-accent', 'ring-offset-2'));
        }

        // Resetear Orientación
        this.state.filterOrientation = 'All';
        const orientSection = Array.from(document.querySelectorAll('h4')).find(el => el.textContent.trim() === 'Orientation');
        if (orientSection) {
            const buttons = orientSection.nextElementSibling.querySelectorAll('button');
            buttons.forEach(b => {
                b.classList.remove('border-accent', 'bg-accent', 'text-white');
                b.classList.add('border-border', 'text-text-secondary');
                if (b.textContent.trim() === 'All') {
                    b.classList.remove('border-border', 'text-text-secondary');
                    b.classList.add('border-accent', 'bg-accent', 'text-white');
                }
            });
        }

        // Limpiar input de búsqueda
        if (this.dom.searchInput) this.dom.searchInput.value = '';

        this.triggerSearch();
    },

    async fetchWallpapers(query) {
        // Loading state
        this.dom.grid.innerHTML = `
            <div class="col-span-full flex justify-center py-12">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-accent"></div>
            </div>`;

        try {
            // Conexión al endpoint del backend
            const response = await fetch(`/api/wallpapers/search?query=${encodeURIComponent(query)}`);
            
            if (!response.ok) {
                console.warn("Backend not reachable");
                this.state.wallpapers = [];
                this.render();
                return;
            }

            const data = await response.json();
            console.log("--- DEBUG: Datos recibidos del backend ---", data);

            if (!Array.isArray(data)) {
                console.error("Error: Se esperaba un array pero se recibió:", data);
                this.state.wallpapers = [];
                this.render();
                return;
            }

            // Mapeo de datos desde WallpaperItem (Backend)
            this.state.wallpapers = data.map((item) => {
                // Intentamos parsear el contenido guardado (asumiendo que es el JSON del scraper)
                let props = {};
                try {
                    if (item.htmlContent) {
                        props = typeof item.htmlContent === 'string' ? JSON.parse(item.htmlContent) : item.htmlContent;
                    }
                } catch (e) {
                    console.warn("Error parseando contenido del wallpaper", e);
                }
                
                // IMPORTANTE: Si props es null (por error de parseo o dato vacío), asignamos objeto vacío para no romper el render
                if (!props) props = {};

                return {
                    id: item.id, // ID de base de datos (puede ser null si viene directo del scraper)
                    title: props.titulo || 'Untitled',
                    thumbnailUrl: props.preview || 'https://img.rocket.new/generatedImages/rocket_gen_img_104abccaa-1764813785194.png',
                    fullUrl: item.url || props.enlace || '#',
                    category: props.sitio || props.tipo || 'Web',
                    resolution: props.resolucion || props.info || '',
                    hasVideo: props.hasVideo === true || props.hasVideo === 'true',
                    rawContent: item.htmlContent // Guardamos el contenido original para enviarlo de vuelta al interactuar
                };
            });

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
                                    ${data.id ? 'Popular' : 'New'}
                                </span>
                            </div>
                            <span class="text-xs">${data.resolution}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        div.addEventListener('click', () => this.openPreview(data));
        
        // Interceptar clic en el enlace del título para registrar redirección
        const link = div.querySelector('a');
        if (link) {
            link.addEventListener('click', (e) => {
                e.stopPropagation(); // Evitar abrir el modal
                this.trackInteraction(data, 'redirect');
            });
        }

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
        // Usar thumbnail inicialmente para evitar imagen rota si fullUrl es una página web
        img.src = data.thumbnailUrl;
        
        const titleEl = document.getElementById('previewTitle');
        titleEl.innerHTML = `<a href="${data.fullUrl}" target="_blank" class="hover:text-accent transition-colors">${data.title}</a>`;
        // Registrar redirección al hacer clic en el título del modal
        titleEl.querySelector('a').onclick = () => this.trackInteraction(data, 'redirect');
        
        document.getElementById('previewCreator').textContent = data.category;
        
        // Configurar el botón de descarga para abrir el enlace original
        const downloadBtn = modal.querySelector('.btn-accent');
        if (downloadBtn) {
            // Clonar el botón para eliminar event listeners anteriores
            const newBtn = downloadBtn.cloneNode(true);
            downloadBtn.parentNode.replaceChild(newBtn, downloadBtn);
            newBtn.onclick = (e) => {
                e.preventDefault();
                console.log("--- DEBUG: Click en botón Download ---");
                console.log("--- DEBUG: Datos del wallpaper:", data);
                this.trackInteraction(data, 'download');
                if (data.fullUrl && data.fullUrl !== '#') window.open(data.fullUrl, '_blank');
                else console.warn("--- DEBUG: URL inválida para descarga ---");
            };
        }
        
        modal.classList.remove('hidden');
        modal.classList.add('flex');

        // Buscar detalles del contenido (Video para MoeWalls, Imagen Full para Wallhaven)
        this.fetchMediaContent(data);
    },

    async fetchMediaContent(data) {
        try {
            // Llamada al nuevo endpoint que usa SiteDetailScraper
            const response = await fetch(`/scraper/details?url=${encodeURIComponent(data.fullUrl)}&site=${encodeURIComponent(data.category)}`);
            if (response.ok) {
                const details = await response.json();
                
                // Caso 1: Video (MoeWalls)
                if (details.videoUrl) {
                    const img = document.getElementById('previewImage');
                    const video = document.createElement('video');
                    
                    video.src = details.videoUrl;
                    video.className = "block w-auto max-w-full max-h-[60vh] mx-auto rounded-t-xl";
                    video.controls = true;
                    video.autoplay = true;
                    video.loop = true;
                    video.muted = false;
                    
                    img.classList.add('hidden');
                    img.parentElement.insertBefore(video, img);
                    
                    this.updateDownloadButton(data, details.videoUrl);
                } 
                // Caso 2: Imagen Full (Wallhaven)
                else if (details.fullImageUrl) {
                    const img = document.getElementById('previewImage');
                    img.src = details.fullImageUrl;
                    
                    this.updateDownloadButton(data, details.fullImageUrl);
                }
            }
        } catch (e) {
            console.error("Error cargando contenido media:", e);
        }
    },

    updateDownloadButton(data, directUrl) {
        const downloadBtn = this.dom.previewModal.querySelector('.btn-accent');
        if (downloadBtn) {
            const newBtn = downloadBtn.cloneNode(true);
            downloadBtn.parentNode.replaceChild(newBtn, downloadBtn);
            newBtn.onclick = (e) => {
                e.preventDefault();
                console.log("--- DEBUG: Descargando recurso directo ---", directUrl);
                this.trackInteraction(data, 'download');
                window.open(directUrl, '_blank');
            };
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

    handleSearchInput() {
        clearTimeout(this.state.searchTimeout);
        const value = this.dom.searchInput.value.trim();
        
        if (value.length > 0) {
            this.dom.searchSuggestions?.classList.remove('hidden');
        } else {
            this.dom.searchSuggestions?.classList.add('hidden');
        }

        this.state.searchTimeout = setTimeout(() => {
            this.triggerSearch();
        }, 600);
    },

    // Nueva función para registrar interacciones en el backend
    async trackInteraction(data, type) {
        console.log(`--- DEBUG: Enviando interacción (${type}) ---`, data.fullUrl);
        try {
            const response = await fetch('/api/wallpapers/interact', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    url: data.fullUrl,
                    htmlContent: typeof data.rawContent === 'string' ? data.rawContent : JSON.stringify(data.rawContent || {}),
                    type: type
                })
            });
            if (response.ok) {
                console.log(`Interaction recorded successfully: ${type}`);
            } else {
                console.error(`Interaction failed: Server returned ${response.status}`);
            }
        } catch (e) {
            console.error("Error recording interaction:", e);
        }
    }
};

document.addEventListener('DOMContentLoaded', () => BrowseModule.init());

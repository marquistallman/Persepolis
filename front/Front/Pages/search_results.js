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
                        <h3 class="text-white font-semibold text-lg mb-2">${data.title}</h3>
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
        // Logic to populate and show modal
        const modal = this.dom.previewModal;
        document.getElementById('previewImage').src = data.fullUrl || data.thumbnailUrl;
        document.getElementById('previewTitle').textContent = data.title;
        // ... populate other fields
        modal.classList.remove('hidden');
        modal.classList.add('flex');
    },

    closeModal() {
        this.dom.previewModal.classList.add('hidden');
        this.dom.previewModal.classList.remove('flex');
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

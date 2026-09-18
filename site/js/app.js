/**
 * Ammora Documentation - Application Router, Theme, Language & Search Engine
 */

(function() {
  // Application State
  const state = {
    lang: localStorage.getItem("ammora_lang") || (navigator.language.startsWith("ru") ? "ru" : "en"),
    theme: localStorage.getItem("ammora_theme") || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light"),
    currentSection: "overview",
    searchSelectedIndex: -1,
    searchResults: []
  };

  window.currentAppLang = state.lang;

  // Cache DOM Elements
  const DOM = {
    html: document.documentElement,
    themeToggleBtn: document.getElementById("theme-toggle-btn"),
    themeIconContainer: document.getElementById("theme-icon-container"),
    langToggleBtn: document.getElementById("lang-toggle-btn"),
    langLabel: document.getElementById("lang-label"),
    sidebarNav: document.getElementById("sidebar-nav"),
    mobileNavToggle: document.getElementById("mobile-nav-toggle"),
    docsSidebar: document.getElementById("docs-sidebar"),
    sidebarBackdrop: document.getElementById("sidebar-backdrop"),
    breadcrumbs: document.getElementById("breadcrumbs"),
    articleTitle: document.getElementById("article-title"),
    articleSubtitle: document.getElementById("article-subtitle"),
    articleBody: document.getElementById("article-body"),
    tocList: document.getElementById("toc-list"),
    tocTitle: document.getElementById("toc-title"),
    pageNav: document.getElementById("page-nav"),
    searchTriggerBtn: document.getElementById("search-trigger-btn"),
    searchModalOverlay: document.getElementById("search-modal-overlay"),
    searchModalInput: document.getElementById("search-modal-input"),
    searchResults: document.getElementById("search-results"),
    searchPlaceholderText: document.getElementById("search-placeholder-text")
  };

  // SVGs for clean icon rendering (no emojis)
  const ICONS = {
    "book-open": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/></svg>`,
    "trending-up": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>`,
    "activity": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>`,
    "monitor": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>`,
    "package": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="16.5" y1="9.4" x2="7.5" y2="4.21"/><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>`,
    "credit-card": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>`,
    "shopping-bag": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>`,
    "cpu": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><line x1="9" y1="1" x2="9" y2="4"/><line x1="15" y1="1" x2="15" y2="4"/><line x1="9" y1="20" x2="9" y2="23"/><line x1="15" y1="20" x2="15" y2="23"/><line x1="20" y1="9" x2="23" y2="9"/><line x1="20" y1="14" x2="23" y2="14"/><line x1="1" y1="9" x2="4" y2="9"/><line x1="1" y1="14" x2="4" y2="14"/></svg>`,
    "settings": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>`,
    "file-text": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`,
    "layers": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/></svg>`,
    "shield": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
    "globe": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`,
    "help-circle": `<svg class="sidebar-nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
    "sun": `<svg class="theme-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>`,
    "moon": `<svg class="theme-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`
  };

  function getDocsData() {
    return state.lang === "ru" ? window.DOCS_RU : window.DOCS_EN;
  }

  // =========================================================================
  // Theme Management
  // =========================================================================
  function applyTheme(theme) {
    state.theme = theme;
    DOM.html.setAttribute("data-theme", theme);
    localStorage.setItem("ammora_theme", theme);
    DOM.themeIconContainer.innerHTML = theme === "dark" ? ICONS.sun : ICONS.moon;
  }

  DOM.themeToggleBtn.addEventListener("click", () => {
    applyTheme(state.theme === "dark" ? "light" : "dark");
  });

  // =========================================================================
  // Language Management
  // =========================================================================
  function applyLanguage(lang) {
    state.lang = lang;
    window.currentAppLang = lang;
    localStorage.setItem("ammora_lang", lang);
    DOM.langLabel.textContent = lang === "ru" ? "RU" : "EN";

    const data = getDocsData();
    DOM.searchPlaceholderText.textContent = data.searchPlaceholder;
    DOM.tocTitle.textContent = data.onThisPage;

    renderSidebar();
    renderCurrentSection();
  }

  DOM.langToggleBtn.addEventListener("click", () => {
    applyLanguage(state.lang === "ru" ? "en" : "ru");
  });

  // =========================================================================
  // Sidebar Rendering
  // =========================================================================
  function renderSidebar() {
    const data = getDocsData();
    let html = "";

    data.categories.forEach(cat => {
      html += `
        <div class="sidebar-category">
          <div class="sidebar-category-header">${cat.title}</div>
          <ul class="sidebar-nav-list">
      `;

      cat.items.forEach(item => {
        const isActive = item.id === state.currentSection;
        const iconSvg = ICONS[item.icon] || ICONS["book-open"];
        html += `
          <li class="sidebar-nav-item">
            <a href="#${item.id}" class="sidebar-nav-link ${isActive ? 'active' : ''}" data-section="${item.id}">
              ${iconSvg}
              <span>${item.title}</span>
            </a>
          </li>
        `;
      });

      html += `
          </ul>
        </div>
      `;
    });

    DOM.sidebarNav.innerHTML = html;

    // Attach click listeners
    DOM.sidebarNav.querySelectorAll(".sidebar-nav-link").forEach(link => {
      link.addEventListener("click", (e) => {
        const sectionId = link.dataset.section;
        if (sectionId) {
          state.currentSection = sectionId;
          renderCurrentSection();
          closeMobileSidebar();
        }
      });
    });
  }

  // =========================================================================
  // Section Content Rendering
  // =========================================================================
  function renderCurrentSection() {
    const data = getDocsData();
    const section = data.sections[state.currentSection] || data.sections["overview"];

    // Update breadcrumbs
    let breadcrumbHtml = `<span class="breadcrumb-item">Ammora</span>`;
    section.breadcrumbs.forEach((b, idx) => {
      breadcrumbHtml += `
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-item ${idx === section.breadcrumbs.length - 1 ? 'active' : ''}">${b}</span>
      `;
    });
    DOM.breadcrumbs.innerHTML = breadcrumbHtml;

    // Update Header
    DOM.articleTitle.textContent = section.title;
    DOM.articleSubtitle.textContent = section.subtitle || "";

    // Render Body HTML
    DOM.articleBody.innerHTML = section.html;

    // Highlight active link in sidebar
    DOM.sidebarNav.querySelectorAll(".sidebar-nav-link").forEach(link => {
      if (link.dataset.section === state.currentSection) {
        link.classList.add("active");
      } else {
        link.classList.remove("active");
      }
    });

    // Mount AMM Calculator if required
    if (state.currentSection === "calculator" || state.currentSection === "amm-economics") {
      if (window.initAmmCalculator) {
        window.initAmmCalculator("amm-calc-mount", state.lang);
      }
    }

    // Build Table of Contents & setup scroll tracking
    buildTableOfContents();

    // Render Previous/Next page navigation footer
    renderPageNav();

    // Scroll top
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  // =========================================================================
  // Table of Contents (On this page)
  // =========================================================================
  function buildTableOfContents() {
    const headings = DOM.articleBody.querySelectorAll("h2, h3");
    if (!headings || headings.length === 0) {
      DOM.tocList.innerHTML = `<li class="toc-item"><span class="toc-link">-</span></li>`;
      return;
    }

    let tocHtml = "";
    headings.forEach((h, index) => {
      if (!h.id) {
        h.id = "heading-" + index;
      }
      const isH3 = h.tagName.toLowerCase() === "h3";
      tocHtml += `
        <li class="toc-item ${isH3 ? 'nested' : ''}">
          <a href="#${h.id}" class="toc-link" data-target="${h.id}">${h.textContent}</a>
        </li>
      `;
    });
    DOM.tocList.innerHTML = tocHtml;

    // Active heading tracking on scroll
    setupTocScrollSpy(headings);
  }

  function setupTocScrollSpy(headings) {
    const links = DOM.tocList.querySelectorAll(".toc-link");
    if (!links || links.length === 0) return;

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const id = entry.target.id;
          links.forEach(l => {
            if (l.getAttribute("href") === "#" + id) {
              l.classList.add("active");
            } else {
              l.classList.remove("active");
            }
          });
        }
      });
    }, {
      rootMargin: "-80px 0px -70% 0px",
      threshold: 0
    });

    headings.forEach(h => observer.observe(h));
  }

  // =========================================================================
  // Page Navigation Footer (Prev / Next)
  // =========================================================================
  function renderPageNav() {
    const data = getDocsData();
    // Flatten items
    const flatItems = [];
    data.categories.forEach(cat => {
      cat.items.forEach(item => flatItems.push(item));
    });

    const currentIndex = flatItems.findIndex(i => i.id === state.currentSection);
    const prevItem = currentIndex > 0 ? flatItems[currentIndex - 1] : null;
    const nextItem = currentIndex < flatItems.length - 1 ? flatItems[currentIndex + 1] : null;

    let html = "";
    if (prevItem) {
      html += `
        <a href="#${prevItem.id}" class="page-nav-link prev" data-section="${prevItem.id}">
          <span class="page-nav-dir">← ${state.lang === 'ru' ? 'Предыдущий раздел' : 'Previous'}</span>
          <span class="page-nav-title">${prevItem.title}</span>
        </a>
      `;
    } else {
      html += `<div></div>`;
    }

    if (nextItem) {
      html += `
        <a href="#${nextItem.id}" class="page-nav-link next" data-section="${nextItem.id}">
          <span class="page-nav-dir">${state.lang === 'ru' ? 'Следующий раздел' : 'Next'} →</span>
          <span class="page-nav-title">${nextItem.title}</span>
        </a>
      `;
    }

    DOM.pageNav.innerHTML = html;

    DOM.pageNav.querySelectorAll(".page-nav-link").forEach(link => {
      link.addEventListener("click", () => {
        state.currentSection = link.dataset.section;
        renderCurrentSection();
      });
    });
  }

  // =========================================================================
  // Search Engine (Client-side, Multi-language)
  // =========================================================================
  function buildSearchIndex() {
    const data = getDocsData();
    const index = [];

    data.categories.forEach(cat => {
      cat.items.forEach(item => {
        const sec = data.sections[item.id];
        if (sec) {
          // Extract plain text from HTML
          const temp = document.createElement("div");
          temp.innerHTML = sec.html;
          const text = temp.textContent || temp.innerText || "";

          index.push({
            id: item.id,
            category: cat.title,
            title: sec.title,
            subtitle: sec.subtitle || "",
            body: text
          });
        }
      });
    });

    return index;
  }

  function openSearchModal() {
    DOM.searchModalOverlay.classList.add("active");
    DOM.searchModalInput.value = "";
    DOM.searchResults.innerHTML = `<div class="search-empty-state">${state.lang === 'ru' ? 'Введите поисковый запрос...' : 'Type to search documentation...'}</div>`;
    state.searchSelectedIndex = -1;
    state.searchResults = [];
    setTimeout(() => DOM.searchModalInput.focus(), 50);
  }

  function closeSearchModal() {
    DOM.searchModalOverlay.classList.remove("active");
  }

  function executeSearch(query) {
    if (!query || query.trim().length < 2) {
      DOM.searchResults.innerHTML = `<div class="search-empty-state">${state.lang === 'ru' ? 'Введите поисковый запрос...' : 'Type to search documentation...'}</div>`;
      state.searchResults = [];
      state.searchSelectedIndex = -1;
      return;
    }

    const q = query.toLowerCase().trim();
    const index = buildSearchIndex();
    const matches = [];

    index.forEach(item => {
      let score = 0;
      let matchSnippet = "";

      if (item.title.toLowerCase().includes(q)) {
        score += 10;
      }
      if (item.subtitle.toLowerCase().includes(q)) {
        score += 5;
      }

      const bodyLower = item.body.toLowerCase();
      const pos = bodyLower.indexOf(q);
      if (pos !== -1) {
        score += 2;
        const start = Math.max(0, pos - 45);
        const end = Math.min(item.body.length, pos + q.length + 65);
        matchSnippet = (start > 0 ? "..." : "") + item.body.substring(start, end) + (end < item.body.length ? "..." : "");
      } else if (item.subtitle) {
        matchSnippet = item.subtitle;
      }

      if (score > 0) {
        matches.push({
          id: item.id,
          category: item.category,
          title: item.title,
          snippet: matchSnippet,
          score: score
        });
      }
    });

    matches.sort((a, b) => b.score - a.score);
    state.searchResults = matches.slice(0, 7);
    state.searchSelectedIndex = state.searchResults.length > 0 ? 0 : -1;

    renderSearchResults(q);
  }

  function renderSearchResults(q) {
    if (state.searchResults.length === 0) {
      DOM.searchResults.innerHTML = `<div class="search-empty-state">${state.lang === 'ru' ? 'Ничего не найдено' : 'No results found'}</div>`;
      return;
    }

    let html = "";
    state.searchResults.forEach((res, idx) => {
      const isSelected = idx === state.searchSelectedIndex;
      // Highlight query
      const highlightedTitle = highlightMatch(res.title, q);
      const highlightedSnippet = highlightMatch(res.snippet, q);

      html += `
        <li class="search-result-item ${isSelected ? 'selected' : ''}" data-idx="${idx}" data-section="${res.id}">
          <div class="search-result-breadcrumb">${res.category}</div>
          <div class="search-result-title">${highlightedTitle}</div>
          <div class="search-result-snippet">${highlightedSnippet}</div>
        </li>
      `;
    });

    DOM.searchResults.innerHTML = html;

    DOM.searchResults.querySelectorAll(".search-result-item").forEach(item => {
      item.addEventListener("click", () => {
        selectSearchResult(parseInt(item.dataset.idx, 10));
      });
    });
  }

  function highlightMatch(text, query) {
    if (!text || !query) return text;
    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, "gi");
    return text.replace(regex, `<span class="search-highlight">$1</span>`);
  }

  function selectSearchResult(idx) {
    const item = state.searchResults[idx];
    if (item) {
      state.currentSection = item.id;
      window.location.hash = item.id;
      renderCurrentSection();
      closeSearchModal();
    }
  }

  // Keyboard Navigation in Search
  DOM.searchModalInput.addEventListener("input", (e) => {
    executeSearch(e.target.value);
  });

  DOM.searchModalInput.addEventListener("keydown", (e) => {
    if (e.key === "ArrowDown") {
      e.preventDefault();
      if (state.searchResults.length > 0) {
        state.searchSelectedIndex = (state.searchSelectedIndex + 1) % state.searchResults.length;
        renderSearchResults(DOM.searchModalInput.value.trim());
      }
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      if (state.searchResults.length > 0) {
        state.searchSelectedIndex = (state.searchSelectedIndex - 1 + state.searchResults.length) % state.searchResults.length;
        renderSearchResults(DOM.searchModalInput.value.trim());
      }
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (state.searchSelectedIndex >= 0) {
        selectSearchResult(state.searchSelectedIndex);
      }
    } else if (e.key === "Escape") {
      closeSearchModal();
    }
  });

  DOM.searchTriggerBtn.addEventListener("click", openSearchModal);
  DOM.searchModalOverlay.addEventListener("click", (e) => {
    if (e.target === DOM.searchModalOverlay) {
      closeSearchModal();
    }
  });

  // Global Shortcuts: Ctrl+K, Cmd+K, or Slash "/"
  window.addEventListener("keydown", (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
      e.preventDefault();
      openSearchModal();
    } else if (e.key === "/" && document.activeElement.tagName !== "INPUT" && document.activeElement.tagName !== "TEXTAREA") {
      e.preventDefault();
      openSearchModal();
    } else if (e.key === "Escape" && DOM.searchModalOverlay.classList.contains("active")) {
      closeSearchModal();
    }
  });

  // =========================================================================
  // Mobile Sidebar Drawer
  // =========================================================================
  function toggleMobileSidebar() {
    const isOpen = DOM.docsSidebar.classList.contains("open");
    if (isOpen) {
      closeMobileSidebar();
    } else {
      DOM.docsSidebar.classList.add("open");
      DOM.sidebarBackdrop.classList.add("active");
    }
  }

  function closeMobileSidebar() {
    DOM.docsSidebar.classList.remove("open");
    DOM.sidebarBackdrop.classList.remove("active");
  }

  DOM.mobileNavToggle.addEventListener("click", toggleMobileSidebar);
  DOM.sidebarBackdrop.addEventListener("click", closeMobileSidebar);

  // =========================================================================
  // Copy Code Snippet Global Helper
  // =========================================================================
  window.copySnippet = function(button) {
    const wrapper = button.closest(".code-block-wrapper");
    if (!wrapper) return;
    const code = wrapper.querySelector("pre.code-block code");
    if (!code) return;

    navigator.clipboard.writeText(code.innerText).then(() => {
      const data = getDocsData();
      const span = button.querySelector("span");
      const prevText = span ? span.textContent : "";
      if (span) span.textContent = data.copied;
      button.classList.add("copied");

      setTimeout(() => {
        if (span) span.textContent = prevText || data.copy;
        button.classList.remove("copied");
      }, 2000);
    });
  };

  // =========================================================================
  // URL Hash Routing
  // =========================================================================
  function handleHashChange() {
    const hash = window.location.hash.replace("#", "");
    if (hash && hash.length > 0) {
      // Check if it's a section
      const data = getDocsData();
      if (data.sections[hash]) {
        state.currentSection = hash;
        renderCurrentSection();
      } else if (document.getElementById(hash)) {
        // Anchor on page
        const el = document.getElementById(hash);
        if (el) el.scrollIntoView({ behavior: "smooth" });
      }
    }
  }

  window.addEventListener("hashchange", handleHashChange);

  // =========================================================================
  // Initial Boot
  // =========================================================================
  applyTheme(state.theme);
  applyLanguage(state.lang);

  if (window.location.hash) {
    handleHashChange();
  }
})();

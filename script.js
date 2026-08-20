(() => {
  document.title = document.title.replace('My Random Resources', 'Railroad Resources');
  const currentScript = document.currentScript;
  if (currentScript?.src && !document.querySelector('link[data-clean-theme]')) {
    const themeLink = document.createElement('link');
    themeLink.rel = 'stylesheet';
    themeLink.href = new URL('clean-theme.css?v=5', currentScript.src).href;
    themeLink.dataset.cleanTheme = 'true';
    document.head.appendChild(themeLink);
  }

  const menuButton = document.querySelector('.menu-button');
  const navigation = document.querySelector('.primary-nav');

  document.querySelectorAll('.brand-copy').forEach((brand) => {
    brand.innerHTML = '<small>Work resource library</small><strong>Railroad Resources</strong>';
  });
  document.querySelectorAll('.footer-brand strong').forEach((brand) => { brand.textContent = 'Railroad Resources'; });
  document.querySelectorAll('.footer-brand span').forEach((tagline) => { tagline.textContent = 'Independent reference library by Loose Bolt Studio.'; });

  if (menuButton && navigation) {
    menuButton.addEventListener('click', () => {
      const isOpen = navigation.classList.toggle('open');
      menuButton.setAttribute('aria-expanded', String(isOpen));
    });

    navigation.querySelectorAll('a').forEach((link) => {
      link.addEventListener('click', () => {
        navigation.classList.remove('open');
        menuButton.setAttribute('aria-expanded', 'false');
      });
    });
  }

  const searchInput = document.querySelector('#resource-search');
  const resourceCards = [...document.querySelectorAll('#resource-grid .resource-card')];
  const noResults = document.querySelector('#no-results');

  if (searchInput && resourceCards.length) {
    searchInput.addEventListener('input', () => {
      const query = searchInput.value.trim().toLowerCase();
      let visibleCount = 0;

      resourceCards.forEach((card) => {
        const searchableText = `${card.textContent} ${card.dataset.search || ''}`.toLowerCase();
        const matches = searchableText.includes(query);
        card.classList.toggle('is-hidden', !matches);
        if (matches) visibleCount += 1;
      });

      if (noResults) noResults.hidden = visibleCount !== 0;
    });
  }

  const year = document.querySelector('#copyright-year');
  if (year) year.textContent = `© ${new Date().getFullYear()}`;
})();

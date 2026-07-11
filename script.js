(() => {
  const menuButton = document.querySelector('.menu-button');
  const navigation = document.querySelector('.primary-nav');

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

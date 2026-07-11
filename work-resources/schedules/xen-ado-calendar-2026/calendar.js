(() => {
  const YEAR = 2026;
  const DAY_MS = 24 * 60 * 60 * 1000;
  const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const LONG_WEEKDAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  const MONTHS = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];
  const DISPLAY_ORDER = ['XEN1B', 'XEN5B', 'XEN4B', 'XEN6B', 'XEN2B', 'XEN3B', 'XEN7B', 'XEN8B'];

  const JOBS = {
    XEN1B: { turn: 1, anchor: '2026-01-05' },
    XEN2B: { turn: 2, anchor: '2026-01-07' },
    XEN3B: { turn: 3, anchor: '2026-01-01' },
    XEN4B: { turn: 4, anchor: '2026-01-03' },
    XEN5B: { turn: 5, anchor: '2026-01-06' },
    XEN6B: { turn: 6, anchor: '2025-12-31' },
    XEN7B: { turn: 7, anchor: '2026-01-02' },
    XEN8B: { turn: 8, anchor: '2026-01-04' }
  };

  const picker = document.querySelector('#job-picker');
  const grid = document.querySelector('#calendar-grid');
  const selectedJob = document.querySelector('#selected-job');
  const selectedDescription = document.querySelector('#selected-description');
  const offDayTotal = document.querySelector('#off-day-total');
  const printButton = document.querySelector('#print-calendar');
  const csvLink = document.querySelector('a[href="xen-ado-2026.csv"]');

  if (!picker || !grid) return;

  function parseDateKey(key) {
    const [year, month, day] = key.split('-').map(Number);
    return Date.UTC(year, month - 1, day);
  }

  function utcFor(year, monthIndex, day) {
    return Date.UTC(year, monthIndex, day);
  }

  function dateKey(year, monthIndex, day) {
    return `${year}-${String(monthIndex + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }

  function isOffDay(jobId, year, monthIndex, day) {
    const anchor = parseDateKey(JOBS[jobId].anchor);
    const current = utcFor(year, monthIndex, day);
    const difference = Math.floor((current - anchor) / DAY_MS);
    const cycleDay = ((difference % 8) + 8) % 8;
    return cycleDay === 0 || cycleDay === 1;
  }

  function getOffDays(jobId) {
    const dates = [];
    for (let month = 0; month < 12; month += 1) {
      const daysInMonth = new Date(YEAR, month + 1, 0).getDate();
      for (let day = 1; day <= daysInMonth; day += 1) {
        if (isOffDay(jobId, YEAR, month, day)) {
          dates.push({ month, day, key: dateKey(YEAR, month, day) });
        }
      }
    }
    return dates;
  }

  function summarizeMonth(jobId, monthIndex) {
    const daysInMonth = new Date(YEAR, monthIndex + 1, 0).getDate();
    const offDays = [];
    for (let day = 1; day <= daysInMonth; day += 1) {
      if (isOffDay(jobId, YEAR, monthIndex, day)) offDays.push(day);
    }

    const ranges = [];
    for (let index = 0; index < offDays.length; index += 1) {
      const start = offDays[index];
      const next = offDays[index + 1];
      if (next === start + 1) {
        ranges.push(`${start}–${next}`);
        index += 1;
      } else {
        ranges.push(String(start));
      }
    }
    return ranges.join(', ');
  }

  function createMonth(jobId, monthIndex) {
    const month = document.createElement('section');
    month.className = 'month-card';
    month.setAttribute('aria-label', `${MONTHS[monthIndex]} ${YEAR}`);

    const header = document.createElement('header');
    header.className = 'month-heading';
    header.innerHTML = `
      <h3>${MONTHS[monthIndex]}</h3>
      <p>Off: ${summarizeMonth(jobId, monthIndex)}</p>
    `;
    month.appendChild(header);

    const weekdayRow = document.createElement('div');
    weekdayRow.className = 'weekday-row';
    WEEKDAYS.forEach((weekday) => {
      const label = document.createElement('span');
      label.textContent = weekday;
      weekdayRow.appendChild(label);
    });
    month.appendChild(weekdayRow);

    const days = document.createElement('div');
    days.className = 'month-days';

    const firstWeekday = new Date(YEAR, monthIndex, 1).getDay();
    for (let blank = 0; blank < firstWeekday; blank += 1) {
      const spacer = document.createElement('span');
      spacer.className = 'calendar-day empty-day';
      spacer.setAttribute('aria-hidden', 'true');
      days.appendChild(spacer);
    }

    const daysInMonth = new Date(YEAR, monthIndex + 1, 0).getDate();
    const today = new Date();

    for (let day = 1; day <= daysInMonth; day += 1) {
      const cell = document.createElement('span');
      const off = isOffDay(jobId, YEAR, monthIndex, day);
      const isToday = today.getFullYear() === YEAR && today.getMonth() === monthIndex && today.getDate() === day;
      const weekday = WEEKDAYS[new Date(YEAR, monthIndex, day).getDay()];

      cell.className = 'calendar-day';
      cell.textContent = day;
      cell.dataset.date = dateKey(YEAR, monthIndex, day);
      if (off) cell.classList.add('off-day');
      if (isToday) cell.classList.add('today');

      const status = off ? ', assigned off day' : '';
      const todayStatus = isToday ? ', today' : '';
      cell.setAttribute('aria-label', `${weekday}, ${MONTHS[monthIndex]} ${day}, ${YEAR}${status}${todayStatus}`);
      days.appendChild(cell);
    }

    month.appendChild(days);
    return month;
  }

  function buildCsvDownload() {
    if (!csvLink) return;

    const rows = [['job_id', 'matrix_turn', 'date', 'day_of_week', 'month']];
    DISPLAY_ORDER.forEach((jobId) => {
      getOffDays(jobId).forEach(({ month, day, key }) => {
        const weekday = LONG_WEEKDAYS[new Date(YEAR, month, day).getDay()];
        rows.push([jobId, JOBS[jobId].turn, key, weekday, MONTHS[month]]);
      });
    });

    const csv = rows.map((row) => row.join(',')).join('\n');
    const blobUrl = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    csvLink.href = blobUrl;
    csvLink.download = 'xen-ado-off-days-2026.csv';
  }

  function render(jobId) {
    const job = JOBS[jobId] || JOBS.XEN1B;
    grid.replaceChildren();

    for (let month = 0; month < 12; month += 1) {
      grid.appendChild(createMonth(jobId, month));
    }

    selectedJob.textContent = jobId;
    selectedDescription.textContent = `Matrix Turn ${job.turn} • 6 days on, 2 assigned days off`;
    offDayTotal.textContent = getOffDays(jobId).length;
    document.title = `${jobId} 2026 Off-Day Calendar | Rail Labor Resource Center`;

    const url = new URL(window.location.href);
    url.searchParams.set('job', jobId);
    window.history.replaceState({}, '', url);
  }

  const requestedJob = new URLSearchParams(window.location.search).get('job');
  const initialJob = requestedJob && JOBS[requestedJob.toUpperCase()]
    ? requestedJob.toUpperCase()
    : 'XEN1B';

  picker.value = initialJob;
  buildCsvDownload();
  render(initialJob);

  picker.addEventListener('change', () => render(picker.value));
  if (printButton) printButton.addEventListener('click', () => window.print());
})();

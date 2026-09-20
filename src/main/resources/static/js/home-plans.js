// Codex 작성: 메인 계획 카드에 plans.js와 같은 분류·정렬을 적용합니다.
// 페이징 없이 선택한 분류의 모든 카드를 가로 스크롤로 표시합니다.
(() => {
  const area = document.querySelector(".home-page .home-plans-content");
  if (!area) return;

  const list = area.querySelector(".my-plan-cards");
  const empty = area.querySelector(".my-plans-empty");
  const tabs = Array.from(area.querySelectorAll(".home-plan-tabs button"));

  // Codex 작성: 화면에서 제외한 카드도 다시 표시할 수 있도록
  // 처음 받은 전체 카드의 참조를 보관합니다.
  const plans = Array.from(list.querySelectorAll("[data-plan-id]"));
  let filter = "all";

  // Codex 작성: 기존 계획 목록과 동일하게 한국 기준 오늘을 구합니다.
  function getToday() {
    const parts = new Intl.DateTimeFormat("en", {
      timeZone: "Asia/Seoul",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).formatToParts(new Date());

    const value = (type) =>
      parts.find((part) => part.type === type).value;

    return `${value("year")}-${value("month")}-${value("day")}`;
  }

  // Codex 작성: 예정·진행 중 → 날짜 둘 다 없음 → 지난 여행 순서입니다.
  function getPlanOrder(plan, today) {
    const date = plan.dataset.endDate || plan.dataset.startDate;

    if (!date) return 1;
    if (date < today) return 2;
    return 0;
  }

  // Codex 작성: 같은 분류에서는 시작일 오름차순,
  // 시작일이 없으면 뒤로, 시작일이 같으면 ID 내림차순으로 정렬합니다.
  function comparePlans(a, b, today) {
    const orderA = getPlanOrder(a, today);
    const orderB = getPlanOrder(b, today);

    if (orderA !== orderB) {
      return orderA - orderB;
    }

    const startA = a.dataset.startDate || "";
    const startB = b.dataset.startDate || "";

    if (!startA && startB) return 1;
    if (startA && !startB) return -1;

    return startA.localeCompare(startB)
      || b.dataset.planId.localeCompare(
        a.dataset.planId, "en", { numeric: true }
      );
  }

  // Codex 작성: 기존 plans.js의 필터 기준을 그대로 적용합니다.
  function matchesFilter(plan, today) {
    const start = plan.dataset.startDate;
    const end = plan.dataset.endDate;

    if (filter === "all") return true;
    if (filter === "undated") return !start || !end;
    if (!start && !end) return false;

    const date = end || start;
    const isPast = date < today;

    return filter === "upcoming" ? !isPast : isPast;
  }

  // Codex 작성: 선택한 분류에 해당하는 카드를 정렬해 다시 배치합니다.
  function render() {
    const today = getToday();

    const filtered = plans
      .filter((plan) => matchesFilter(plan, today))
      .sort((a, b) => comparePlans(a, b, today));

    list.replaceChildren(...filtered);
    empty.hidden = filtered.length > 0;

    // Codex 작성: 분류를 바꾸면 가로 스크롤을 처음으로 돌립니다.
    list.scrollLeft = 0;

    tabs.forEach((tab) => {
      const active = tab.dataset.filter === filter;
      tab.classList.toggle("active", active);
      tab.setAttribute("aria-pressed", String(active));
    });
  }

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      filter = tab.dataset.filter;
      render();
    });
  });

  render();
})();
// Codex 작성: 전체 계획을 받아 브라우저에서 분류·시작일 정렬·10개씩 표시합니다.
(() => {
  const page = document.querySelector(".plans-page");
  if (!page) return;

  const list = page.querySelector(".plan-list");
  const empty = page.querySelector(".plans-empty");
  const pagination = page.querySelector(".plans-pagination");
  const tabs = Array.from(page.querySelectorAll(".plan-tabs a"));
  const plans = Array.from(list.querySelectorAll("[data-plan-id]"));
  const pageSize = 10;
  const params = new URL(window.location.href).searchParams;
  let filter = params.get("filter") || "all";
  let currentPage = Number(params.get("pageNo") || 1);

  if (!["all", "upcoming", "past", "undated"].includes(filter)) filter = "all";
  if (!Number.isSafeInteger(currentPage) || currentPage < 1) currentPage = 1;

function getPlanOrder(plan, today) {
  const date = plan.dataset.endDate || plan.dataset.startDate;

  if (!date) return 1;         // 날짜 둘 다 없음: 중간
  if (date < today) return 2;  // 지난 여행: 맨 아래
  return 0;                   // 예정·진행중: 맨 위
}

function comparePlans(a, b, today) {
  // 1. 여행 분류 순서로 정렬
  const orderA = getPlanOrder(a, today);
  const orderB = getPlanOrder(b, today);

  if (orderA !== orderB) {
    return orderA - orderB;
  }

  // 2. 같은 분류 안에서는 시작일 오름차순
  const startA = a.dataset.startDate || "";
  const startB = b.dataset.startDate || "";

  if (!startA && startB) return 1;
  if (startA && !startB) return -1;

  // 3. 시작일도 같으면 ID가 큰 계획부터 표시
  return startA.localeCompare(startB)
    || b.dataset.planId.localeCompare(
      a.dataset.planId, "en", { numeric: true }
    );
}

  // Codex 작성: 브라우저의 지역 설정과 관계없이 한국 기준 오늘을 YYYY-MM-DD로 구합니다.
  function getToday() {
    const parts = new Intl.DateTimeFormat("en", {
      timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit",
    }).formatToParts(new Date());
    const value = (type) => parts.find((part) => part.type === type).value;
    return `${value("year")}-${value("month")}-${value("day")}`;
  }

  // Codex 작성: 날짜가 하나라도 없으면 날짜 미정, 두 날짜가 있으면 종료일로 분류합니다.
  // function matchesFilter(plan, today) {
  //   const start = plan.dataset.startDate;
  //   const end = plan.dataset.endDate;
  //   if (filter === "all") return true;
  //   if (filter === "undated") return !start || !end;
  //   if (!start || !end) return false;
  //   return filter === "upcoming" ? end >= today : end < today;
  // }


  function matchesFilter(plan, today) {
  const start = plan.dataset.startDate;
  const end = plan.dataset.endDate;

  if (filter === "all") return true;

  // 날짜가 하나라도 없으면 날짜 미정에도 표시
  if (filter === "undated") return !start || !end;

  // 두 날짜 모두 없으면 예정·진행중 / 지난 여행에서는 제외
  if (!start && !end) return false;

  // “종료일이 있으면 종료일 기준, 시작일만 있으면 시작일 기준”
const date = end || start;
  const isPast = date < today;

  return filter === "upcoming" ? !isPast : isPast;
}

  // Codex 작성: 서버 요청 없이 선택한 분류의 현재 페이지와 페이지 버튼을 표시합니다.
function render() {
  const today = getToday();

  const filtered = plans
    .filter((plan) => matchesFilter(plan, today))
    .sort((a, b) => comparePlans(a, b, today));

  // 이 아래 코드는 기존 그대로
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    currentPage = Math.min(currentPage, totalPages);
    const start = (currentPage - 1) * pageSize;
    const visible = filtered.slice(start, start + pageSize);
    visible.forEach((plan) => { plan.hidden = false; });
    empty.hidden = filtered.length > 0;
    list.replaceChildren(...visible, empty);

    tabs.forEach((tab) => {
      const active = tab.dataset.filter === filter;
      tab.classList.toggle("active", active);
      if (active) tab.setAttribute("aria-current", "page");
      else tab.removeAttribute("aria-current");
    });

    pagination.replaceChildren();
    if (currentPage > 1) addPageButton("이전", currentPage - 1);
    for (let number = 1; number <= totalPages; number++) {
      addPageButton(String(number), number, number === currentPage);
    }
    if (currentPage < totalPages) addPageButton("다음", currentPage + 1);

    // 새로고침해도 선택한 분류와 페이지가 유지됩니다.
    const url = new URL(window.location.href);
    url.searchParams.set("filter", filter);
    url.searchParams.set("pageNo", String(currentPage));
    window.history.replaceState(null, "", url);
  }

  // Codex 작성: 페이지 버튼은 받은 목록만 다시 표시하므로 API를 호출하지 않습니다.
  function addPageButton(label, number, active = false) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = label;
    if (active) {
      button.classList.add("active");
      button.setAttribute("aria-current", "page");
    }
    button.addEventListener("click", () => {
      currentPage = number;
      render();
    });
    pagination.append(button);
  }

  // Codex 작성: 분류를 바꾸면 해당 분류의 첫 페이지부터 표시합니다.
  tabs.forEach((tab) => tab.addEventListener("click", (event) => {
    if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    filter = tab.dataset.filter;
    currentPage = 1;
    render();
  }));
  render();
})();

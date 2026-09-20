// Codex 작성: 각 일정의 상세 영역을 독립적으로 펼치거나 접습니다.
(() => {
  const section = document.getElementById("schedule-order");
  if (!section) return;

  section.addEventListener("click", (event) => {
    const button = event.target.closest(".schedule-detail-toggle");
    if (!button || !section.contains(button)) return;

    const detailId = button.getAttribute("aria-controls");
    const detail = document.getElementById(detailId);
    if (!detail || !section.contains(detail)) return;

    const expanded = button.getAttribute("aria-expanded") === "true";

    detail.hidden = expanded;
    button.setAttribute("aria-expanded", String(!expanded));
    button.textContent = expanded ? "상세" : "상세 접기";
  });
})();
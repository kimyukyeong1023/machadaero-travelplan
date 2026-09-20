// 페이지 이동으로 나중에 추가된 상세보기 버튼도 이곳에서 처리한다.
document.addEventListener("click", (event) => {
  const button = event.target.closest(".place-detail-button");
  if (!button || button.disabled) return;
  showPlaceDetail(button);
});

// 상세보기 버튼 → 해당 li → 상세 영역 순서로 찾는다.
async function showPlaceDetail(button) {
  const listItem = button.closest("li");
  const detail = listItem.querySelector(".place-detail");
  const message = listItem.querySelector(".detail-message");

  // 이미 열린 상세 정보는 요청 없이 닫는다.
  if (!detail.hidden) {
    detail.hidden = true;
    button.textContent = "상세보기";
    button.setAttribute("aria-expanded", "false");
    return;
  }

  // 요청 중에는 중복 클릭을 막는다.
  button.disabled = true;
  button.textContent = "불러오는 중…";
  if (message) message.textContent = "";

  try {
    // Thymeleaf가 만든 URL에는 애플리케이션의 context-path도 반영된다.
    // 기존 버튼에서 호출할 경우 기존 data-contentid로도 동작한다.
    const url = button.dataset.detailUrl
      || `/api/places/${encodeURIComponent(button.dataset.contentid)}`;
    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`상세 조회 실패: ${response.status}`);
    }

    const place = await response.json();
    detail.querySelector(".detail-title").textContent =
      place.title || "이름 정보 없음";
    detail.querySelector(".detail-address").textContent =
      [place.addr1, place.addr2].filter(Boolean).join(" ") || "주소 정보가 없습니다.";
    detail.querySelector(".detail-overview").textContent =
      place.overview || "소개 정보가 없습니다.";

    detail.hidden = false;
    button.setAttribute("aria-expanded", "true");
  } catch (error) {
    console.error("상세 조회 중 오류:", error);
    if (message) message.textContent = "상세 정보를 불러오지 못했습니다. 다시 눌러주세요.";
  } finally {
    button.disabled = false;
    button.textContent = detail.hidden ? "상세보기" : "상세 닫기";
  }
}

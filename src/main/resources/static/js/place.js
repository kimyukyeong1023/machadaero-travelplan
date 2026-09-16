async function showPlaceDetail(button) {
  const contentid = button.dataset.contentid;

  // 클릭한 버튼에서 위로 올라가며 가장 가까운 li를 찾음
  const listItem = button.closest("li");

  // 그 li 내부의 상세 영역만 찾음
  const detail = listItem.querySelector(".place-detail");

  try {
    const response = await fetch(
      `/api/places/${encodeURIComponent(contentid)}`,
    );

    if (!response.ok) {
      throw new Error(`상세 조회 실패: ${response.status}`);
    }

    const place = await response.json();

    detail.querySelector(".detail-title").textContent =
      place.title ?? "이름 정보 없음";

    detail.querySelector(".detail-address").textContent = [
      place.addr1,
      place.addr2,
    ]
      .filter(Boolean)
      .join(" ");

    detail.querySelector(".detail-overview").textContent =
      place.overview || "소개 정보가 없습니다.";

    detail.hidden = false;
  } catch (error) {
    console.error("상세 조회 중 오류:", error);
  }
}

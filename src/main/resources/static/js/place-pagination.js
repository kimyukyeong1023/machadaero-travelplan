// 외부 API의 한 페이지(25개)를 화면의 최대 다섯 페이지(각 5개)로 나눈다.
(() => {
  const section = document.querySelector(".search-results");
  if (!section) return;

  const pageSize = 5;
  const batchSize = 25;
  const pagesPerBatch = batchSize / pageSize;
  const list = section.querySelector(".place-list");
  const pagination = section.querySelector(".page-buttons");
  const numbers = section.querySelector(".page-numbers");
  const previous = section.querySelector(".previous-pages");
  const next = section.querySelector(".next-pages");
  const message = section.querySelector(".pagination-message");
  const empty = section.querySelector(".search-empty");
  const count = section.querySelector(".search-count");
// Codex 수정: data-keyword 값이 없으면 빈 문자열을 사용합니다.
// undefined가 "undefined"라는 검색어로 전송되는 것을 방지합니다.
const keyword = section.dataset.keyword || "";
const searchUrl = section.dataset.searchUrl;

// Codex 수정: 현재 검색 결과에 적용된 지역·카테고리를 저장합니다.
// 검색 후 사용자가 select만 바꿔도 기존 결과의 다음 페이지 조건은 유지됩니다.
const lDongRegnCd = section.dataset.lDongRegnCd || "";
const lclsSystm1 = section.dataset.lclsSystm1 || "";

  let batchNo = Number(section.dataset.batchNo);
  let totalCount = Number(section.dataset.totalCount);
  let localPage = 0;
  let loading = false;
  // 현재 검색 화면에서 받아 둔 묶음만 보관한다. 새 검색/새로고침 시 초기화된다.
  const batches = new Map([
    [batchNo, Array.from(list.querySelectorAll(".place-item"))],
  ]);

  function updateControls() {
    previous.disabled = loading || !batches.has(batchNo - 1);
    next.disabled = loading || batchNo * batchSize >= totalCount
      || batches.get(batchNo).length === 0;
    numbers.querySelectorAll("button").forEach((button) => {
      button.disabled = loading;
    });
    section.setAttribute("aria-busy", String(loading));
  }

  function render() {
    const items = batches.get(batchNo);
    list.replaceChildren(...items);
    items.forEach((item, index) => {
      item.hidden = Math.floor(index / pageSize) !== localPage;
    });
    empty.hidden = items.length > 0;
    pagination.hidden = totalCount === 0;
    count.textContent = `총 ${totalCount}건`;
    numbers.replaceChildren();

    const pageCount = Math.min(pagesPerBatch, Math.ceil(items.length / pageSize));
    for (let index = 0; index < pageCount; index++) {
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = String((batchNo - 1) * pagesPerBatch + index + 1);
      if (index === localPage) button.setAttribute("aria-current", "page");
      button.addEventListener("click", () => {
        if (loading) return;
        localPage = index;
        message.textContent = "";
        render();
      });
      numbers.append(button);
    }
    updateControls();
  }

  async function moveBatch(target) {
    if (loading || target < 1) return;
    message.textContent = "";
    if (batches.has(target)) {
      batchNo = target;
      localPage = 0;
      render();
      return;
    }

    // 아직 받지 않은 다음 묶음에서만 서버에 요청한다.
    loading = true;
    message.textContent = "검색 결과를 불러오는 중…";
    updateControls();
    try {
const url = new URL(searchUrl, window.location.href);
url.searchParams.set("keyword", keyword);
url.searchParams.set("pageNo", String(target));

// Codex 수정: 다음 묶음을 요청할 때도 동일한 지역·카테고리를 전달합니다.
url.searchParams.set("lDongRegnCd", lDongRegnCd);
url.searchParams.set("lclsSystm1", lclsSystm1);

const response = await fetch(url);
      if (!response.ok) throw new Error("검색 결과 조회 실패");

      const documentResult = new DOMParser().parseFromString(await response.text(), "text/html");
      const result = documentResult.querySelector(".search-results");
      if (!result || Number(result.dataset.batchNo) !== target
          || !result.querySelector(".place-list")
          || !Number.isFinite(Number(result.dataset.totalCount))) {
        throw new Error("검색 결과 응답 형식 오류");
      }

      batches.set(target, Array.from(result.querySelectorAll(".place-item")));
      totalCount = Number(result.dataset.totalCount);
      batchNo = target;
      localPage = 0;
      message.textContent = "";
      render();
    } catch (error) {
      console.error("페이지 조회 중 오류:", error);
      message.textContent = "검색 결과를 불러오지 못했습니다. 다음 버튼을 눌러 다시 시도해 주세요.";
    } finally {
      loading = false;
      updateControls();
    }
  }

  previous.addEventListener("click", () => {
    if (!previous.disabled) moveBatch(batchNo - 1);
  });
  next.addEventListener("click", () => {
    if (!next.disabled) moveBatch(batchNo + 1);
  });
  render();
})();

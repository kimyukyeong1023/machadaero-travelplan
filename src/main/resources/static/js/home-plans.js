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

    const value = (type) => parts.find((part) => part.type === type).value;

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

    return (
      startA.localeCompare(startB) ||
      b.dataset.planId.localeCompare(a.dataset.planId, "en", { numeric: true })
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

  // Codex 추가: 카드 선택에 따라 검색 결과 영역을 좌우 배치로 전환합니다.
  // Codex 수정: 카드 선택 시 오른쪽 영역을 열고 해당 계획의 일정을 조회합니다.
  // Codex 수정: 계획 선택·일정 추가·순서 편집을 한곳에서 관리합니다.
  const home = document.querySelector(".home-page");
  const contentArea = home.querySelector(".content-area");
  const panel = home.querySelector(".selected-plan-panel");
  const panelTitle = panel.querySelector(".selected-plan-title");
  const status = panel.querySelector(".selected-plan-status");
  const itemList = panel.querySelector(".selected-plan-items");

  const editButton = panel.querySelector(".home-order-edit");
  const saveButton = panel.querySelector(".home-order-save");
  const cancelButton = panel.querySelector(".home-order-cancel");
  const reloadButton = panel.querySelector(".home-items-reload");

  let selectedPlanId = null;
  let itemsUrl = "";
  let orderUrl = "";

  let loading = false;
  let adding = false;
  let saving = false;
  let editing = false;
  let loaded = false;
  let conflict = false;

  let originalRows = [];
  let originalNumbers = [];
  let requestVersion = 0;

  const rows = () => Array.from(itemList.children);
  const ids = () => rows().map((row) => row.dataset.orderItemId);
  const originalIds = () => originalRows.map((row) => row.dataset.orderItemId);

  const changed = () => {
    const current = ids();
    const original = originalIds();
    return (
      current.length !== original.length ||
      current.some((id, index) => id !== original[index])
    );
  };

  function showStatus(message) {
    status.textContent = message;
  }

  // Codex 추가: CSRF를 사용하는 프로젝트에서는 POST에 토큰을 함께 보냅니다.
  function jsonHeaders() {
    const headers = {
      "Content-Type": "application/json",
      Accept: "application/json",
    };

    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const name = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (token && name) headers[name] = token;
    return headers;
  }

  function checkResponse(response, expectedStatus) {
    if (response.redirected) {
      throw new Error("로그인 상태를 확인한 뒤 다시 시도해 주세요.");
    }

    if (response.status !== expectedStatus) {
      const messages = {
        400: "요청 내용을 확인해 주세요.",
        401: "로그인이 만료되었습니다. 다시 로그인해 주세요.",
        403: "요청 권한 또는 로그인 보안 토큰을 확인해 주세요.",
        404: "해당 여행계획을 찾을 수 없습니다.",
        409: "일정이 변경되었습니다. 취소 후 최신 목록으로 다시 편집해 주세요.",
      };

      throw new Error(
        messages[response.status] || "요청을 처리하지 못했습니다.",
      );
    }
  }

  function refreshControls() {
    const busy = loading || adding || saving;

    editButton.hidden = editing;
    editButton.disabled = busy || !loaded || rows().length < 2;

    saveButton.hidden = !editing;
    cancelButton.hidden = !editing;
    saveButton.disabled = busy || conflict || !changed();
    cancelButton.disabled = busy;
    saveButton.textContent = saving ? "저장 중…" : "순서 저장";

    reloadButton.hidden = editing;
    reloadButton.disabled = busy || selectedPlanId === null;

    rows().forEach((row, index, items) => {
      const up = row.querySelector('[data-order-move="up"]');
      const down = row.querySelector('[data-order-move="down"]');

      up.hidden = down.hidden = !editing;
      up.disabled = busy || conflict || index === 0;
      down.disabled = busy || conflict || index === items.length - 1;

      // Codex 추가: 순서 편집 중에는 삭제를 숨기고,
      // 서버 처리 중이거나 목록 확인이 필요한 상태에서는 삭제를 막습니다.
      const deleteButton = row.querySelector(".home-item-delete");
      deleteButton.hidden = editing;
      deleteButton.disabled = busy || !loaded;
    });

    panel.setAttribute("aria-busy", String(busy));
  }

  function updatePlanSelection() {
    const selected = selectedPlanId !== null;
    contentArea.classList.toggle("plan-selected", selected);
    panel.hidden = !selected;

    plans.forEach((plan) => {
      plan
        .querySelector(".my-plan-card")
        .setAttribute(
          "aria-pressed",
          String(plan.dataset.planId === selectedPlanId),
        );
    });
  }

  // Codex 수정: 일정 ID와 표시 번호를 별도 요소에 저장해 순서 편집에 사용합니다.
  function createItemRow(item) {
    const row = document.createElement("li");
    row.className = "selected-plan-item";
    row.dataset.orderItemId = String(item.id);

    const title = document.createElement("h3");
    const number = document.createElement("span");
    number.className = "col-order";
    number.textContent = item.sortOrder;

    title.append(
      number,
      document.createTextNode(`. ${item.placeName || "장소명 없음"}`),
    );

    const date = document.createElement("p");
    date.className = "selected-item-date";
    date.textContent = `방문일: ${item.visitDate || "미정"}`;

    const address = document.createElement("p");
    address.className = "selected-item-address";
    address.textContent = item.address || "주소 없음";

    const memo = document.createElement("p");
    memo.className = "selected-item-memo";
    memo.textContent = item.memo || "일정 내용 없음";

    const controls = document.createElement("div");
    controls.className = "selected-item-moves";

    ["up", "down"].forEach((direction) => {
      const button = document.createElement("button");
      button.type = "button";
      button.dataset.orderMove = direction;
      button.textContent = direction === "up" ? "↑" : "↓";
      button.setAttribute(
        "aria-label",
        direction === "up" ? "일정을 위로 이동" : "일정을 아래로 이동",
      );
      button.hidden = true;
      controls.append(button);
    });

    // Codex 추가: 각 일정에 삭제 버튼을 만듭니다.
    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "home-item-delete";
    deleteButton.textContent = "삭제";

    // Codex 수정: 순서 이동 버튼 뒤에 삭제 버튼도 표시합니다.
    row.append(title, date, address, memo, controls, deleteButton);
    return row;
  }

  // Codex 수정: 조회 실패 시 추가·순서 편집을 막고 새로고침으로 복구합니다.
  async function loadItems(successMessage = "") {
    const version = ++requestVersion;
    const url = itemsUrl;

    loading = true;
    loaded = false;
    itemList.replaceChildren();
    showStatus("일정을 불러오는 중…");
    refreshControls();

    try {
      const response = await fetch(url, {
        headers: { Accept: "application/json" },
        cache: "no-store",
      });

      checkResponse(response, 200);

      const items = await response.json();
      if (!Array.isArray(items)) {
        throw new Error("일정 응답 형식이 올바르지 않습니다.");
      }

      if (version !== requestVersion) return;

      itemList.replaceChildren(...items.map(createItemRow));
      loaded = true;

      showStatus(
        successMessage || (items.length ? "" : "등록된 일정이 없습니다."),
      );
    } catch (error) {
      if (version !== requestVersion) return;
      console.error(error);
      showStatus("목록을 불러오지 못했습니다. '목록 새로고침'을 눌러 주세요.");
    } finally {
      if (version === requestVersion) {
        loading = false;
        refreshControls();
      }
    }
  }

  // Codex 수정: 순서 편집·저장·추가 중에는 계획 선택 변경을 막습니다.
  list.addEventListener("click", (event) => {
    const button = event.target.closest("button.my-plan-card");
    if (!button || !list.contains(button)) return;

    if (editing || adding || saving) {
      showStatus("진행 중인 추가를 기다리거나 순서 편집을 저장·취소해 주세요.");
      return;
    }

    const plan = button.closest("[data-plan-id]");
    if (!plan) return;

    ++requestVersion;
    loading = false;
    loaded = false;

    selectedPlanId =
      selectedPlanId === plan.dataset.planId ? null : plan.dataset.planId;

    itemList.replaceChildren();
    showStatus("");
    updatePlanSelection();

    if (selectedPlanId === null) {
      itemsUrl = "";
      orderUrl = "";
      refreshControls();
      return;
    }

    itemsUrl = button.dataset.itemsUrl;
    orderUrl = button.dataset.orderUrl;
    panelTitle.textContent = button.querySelector("h3").textContent.trim();

    if (!itemsUrl || !orderUrl) {
      showStatus("계획 카드의 조회·순서 저장 주소를 확인해 주세요.");
      refreshControls();
      return;
    }

    conflict = false;
    loadItems();
  });

  reloadButton.addEventListener("click", () => {
    if (!editing && !loading && !adding && !saving && selectedPlanId !== null) {
      loadItems();
    }
  });

  // Codex 추가: 검색 결과가 페이지 이동으로 교체되어도 작동하도록
  // 상위 요소에서 '계획에 추가' 클릭을 처리합니다.
  home.addEventListener("click", async (event) => {
    const button = event.target.closest(".place-add-button");
    if (!button || !home.contains(button)) return;

    if (selectedPlanId === null) {
      alert("아래 '내 계획'에서 추가할 계획을 먼저 선택해 주세요.");
      return;
    }

    if (editing || loading || adding || saving || !loaded) {
      showStatus(
        "목록 조회를 완료하거나 순서 편집을 저장·취소한 뒤 추가해 주세요.",
      );
      return;
    }

    adding = true;
    button.disabled = true;
    showStatus("관광지를 일정에 추가하는 중…");
    refreshControls();

    let confirmed = false;

    try {
      const response = await fetch(itemsUrl, {
        method: "POST",
        headers: jsonHeaders(),
        credentials: "same-origin",
        body: JSON.stringify({
          placeName: button.dataset.placeName,
          placeAddress: button.dataset.placeAddress || "",
        }),
      });

      checkResponse(response, 201);
      confirmed = true;

      // Codex 추가: 저장 후 전체 일정을 다시 읽어 실제 저장 순서를 반영합니다.
      await loadItems("계획의 마지막에 관광지를 추가했습니다.");
    } catch (error) {
      console.error(error);

      // Codex 추가: 통신 오류에서는 저장 여부가 불확실할 수 있으므로,
      // 재추가 전에 목록을 다시 확인하도록 합니다.
      if (!confirmed) {
        loaded = false;
        showStatus(
          "추가 완료를 확인하지 못했습니다. '목록 새로고침'으로 저장 여부를 확인한 뒤 다시 시도해 주세요.",
        );
      }
    } finally {
      adding = false;
      button.disabled = false;
      refreshControls();
    }
  });

  // Codex 추가: 편집 전 DOM 순서와 번호를 저장합니다.
  editButton.addEventListener("click", () => {
    if (!loaded || loading || adding || saving || rows().length < 2) return;

    originalRows = rows();
    originalNumbers = originalRows.map(
      (row) => row.querySelector(".col-order").textContent,
    );

    editing = true;
    conflict = false;
    showStatus("화살표로 순서를 바꾼 뒤 '순서 저장'을 눌러 주세요.");
    refreshControls();
  });

  itemList.addEventListener("click", (event) => {
    const button = event.target.closest("[data-order-move]");
    if (!button || !editing || saving || conflict || button.disabled) return;

    const row = button.closest("[data-order-item-id]");
    const neighbor =
      button.dataset.orderMove === "up"
        ? row.previousElementSibling
        : row.nextElementSibling;

    if (!neighbor) return;

    if (button.dataset.orderMove === "up") {
      itemList.insertBefore(row, neighbor);
    } else {
      itemList.insertBefore(neighbor, row);
    }

    rows().forEach((item, index) => {
      item.querySelector(".col-order").textContent = index + 1;
    });

    showStatus(
      changed() ? "아직 저장하지 않은 순서입니다." : "원래 순서입니다.",
    );
    refreshControls();
  });

  cancelButton.addEventListener("click", () => {
    if (saving) return;

    originalRows.forEach((row, index) => {
      itemList.append(row);
      row.querySelector(".col-order").textContent = originalNumbers[index];
    });

    editing = false;

    // Codex 추가: 충돌하거나 저장 결과를 확인하지 못한 경우 최신 목록을 읽습니다.
    if (conflict) {
      conflict = false;
      loadItems();
    } else {
      showStatus("순서 편집을 취소했습니다.");
      refreshControls();
    }
  });

  saveButton.addEventListener("click", async () => {
    if (!editing || saving || conflict || !changed()) return;

    saving = true;
    showStatus("순서를 저장하는 중…");
    refreshControls();

    try {
      const response = await fetch(orderUrl, {
        method: "POST",
        headers: jsonHeaders(),
        credentials: "same-origin",
        body: JSON.stringify({
          originalItemIds: originalIds(),
          itemIds: ids(),
        }),
      });

      if (response.status === 409) conflict = true;
      checkResponse(response, 204);

      editing = false;
      showStatus("일정 순서를 저장했습니다.");
    } catch (error) {
      console.error(error);

      // Codex 추가: 통신 실패 시 서버에 저장됐을 가능성이 있어 최신 목록 확인을 유도합니다.
      if (error instanceof TypeError) {
        conflict = true;
        showStatus(
          "저장 결과를 확인하지 못했습니다. 취소를 눌러 최신 목록을 확인해 주세요.",
        );
      } else {
        showStatus(error.message);
      }
    } finally {
      saving = false;
      refreshControls();
    }
  });

  // Codex 추가: 일반 검색 제출로 편집 내용이 사라지는 것을 막습니다.
  home
    .querySelector("#place-search-form")
    ?.addEventListener("submit", (event) => {
      if (editing || adding || saving) {
        event.preventDefault();
        showStatus("추가가 끝나거나 순서 편집을 저장·취소한 뒤 검색해 주세요.");
        return;
      }

      // Codex 수정: GET 검색 후 새 화면에서도 선택한 계획을 복원합니다.
      // HTML을 수정하지 않고 숨김 입력을 만들어 기존 검색 조건과 함께 보냅니다.
      const form = event.currentTarget;
      let selection = form.querySelector('input[name="selectedPlanId"]');
      if (selectedPlanId === null) {
        selection?.remove();
        return;
      }
      if (!selection) {
        selection = document.createElement("input");
        selection.type = "hidden";
        selection.name = "selectedPlanId";
        form.append(selection);
      }
      selection.value = selectedPlanId;
    });

  // Codex 추가: 저장하지 않은 변경 또는 저장 요청 중에는 페이지 이탈을 확인합니다.
  window.addEventListener("beforeunload", (event) => {
    if (adding || saving || (editing && changed())) {
      event.preventDefault();
      event.returnValue = "";
    }
  });

  // Codex 추가: 동적으로 생성된 일정의 삭제 버튼도 처리합니다.
itemList.addEventListener("click", async (event) => {
  const button = event.target.closest(".home-item-delete");
  if (!button || !itemList.contains(button)) return;

  if (
    selectedPlanId === null
    || editing
    || loading
    || adding
    || saving
    || !loaded
    || button.disabled
  ) {
    return;
  }

  const row = button.closest("[data-order-item-id]");
  if (!row) return;

  if (!window.confirm("이 일정을 삭제하시겠습니까?")) return;

  // Codex 설명: 기존 adding 상태를 추가·삭제 처리 중 공통 잠금으로 재사용합니다.
  // 삭제 중에도 다른 계획 선택, 관광지 추가, 순서 편집을 막습니다.
  adding = true;
  showStatus("일정을 삭제하는 중…");
  refreshControls();

  try {
    const url = `${itemsUrl}/${encodeURIComponent(row.dataset.orderItemId)}`;

    const response = await fetch(url, {
      method: "DELETE",
      headers: jsonHeaders(),
      credentials: "same-origin",
    });

    checkResponse(response, 204);

    // Codex 추가: 삭제가 완료되면 서버에서 최신 목록을 다시 가져옵니다.
    await loadItems("일정을 삭제했습니다.");
  } catch (error) {
    console.error("일정 삭제 오류:", error);

    // Codex 추가: 통신 실패로 삭제 여부가 불확실할 수 있어
    // 최신 목록을 확인하기 전까지 추가 편집을 막습니다.
    loaded = false;
    showStatus(
      "삭제 완료를 확인하지 못했습니다. '목록 새로고침'으로 현재 일정을 확인해 주세요."
    );
  } finally {
    adding = false;
    refreshControls();
  }
});
  refreshControls();
  render();

  // Codex 추가: 현재 사용자의 카드에 있는 계획만 복원합니다.
  // 기존 클릭 처리를 재사용하여 선택 표시, 좌우 배치, 일정 조회를 함께 실행합니다.
  const restoredPlanId = new URL(window.location.href).searchParams.get("selectedPlanId");
  const restoredPlan = plans.find((plan) => plan.dataset.planId === restoredPlanId);
  restoredPlan?.querySelector(".my-plan-card")?.click();
})();

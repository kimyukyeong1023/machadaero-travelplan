// Codex 작성: ↑↓ 이동은 화면에만 반영하고, '순서 저장'을 눌렀을 때 전체 ID 순서를 서버에 보냅니다.
(() => {
  const section = document.getElementById("schedule-order");
  if (!section) return;

  const list = section.querySelector(".schedule-list");
  const editButton = document.getElementById("order-edit");
  const saveButton = document.getElementById("order-save");
  const cancelButton = document.getElementById("order-cancel");
  const status = document.getElementById("order-status");
  const normalControls = Array.from(section.querySelectorAll("[data-order-normal]"));
  let editing = false;
  let saving = false;
  let originalRows = [];
  let originalNumbers = [];
  const rows = () => Array.from(list.children);
  const ids = () => rows().map((row) => row.dataset.orderItemId);
  const originalIds = () => originalRows.map((row) => row.dataset.orderItemId);
  const changed = () => ids().some((id, index) => id !== originalIds()[index]);

  function showStatus(message, error = false) {
    status.textContent = message;
    status.classList.toggle("order-error", error);
  }

  // Codex 작성: 맨 위·맨 아래의 이동을 막고, 저장 중에는 편집 버튼을 잠가 중복 요청을 방지합니다.
  function refreshControls() {
    const items = rows();
    editButton.hidden = editing;
    editButton.disabled = items.length < 2;
    saveButton.hidden = !editing;
    cancelButton.hidden = !editing;
    saveButton.disabled = saving || !changed();
    cancelButton.disabled = saving;
    saveButton.textContent = saving ? "저장 중..." : "순서 저장";
    normalControls.forEach((control) => { control.hidden = editing; });
    items.forEach((row, index) => {
      const up = row.querySelector('[data-order-move="up"]');
      const down = row.querySelector('[data-order-move="down"]');
      up.hidden = down.hidden = !editing;
      up.disabled = saving || index === 0;
      down.disabled = saving || index === items.length - 1;
    });
    section.setAttribute("aria-busy", String(saving));
  }

  editButton.addEventListener("click", () => {
    originalRows = rows();
    originalNumbers = originalRows.map((row) => row.querySelector(".col-order").textContent);
    editing = true;
    showStatus("화살표로 순서를 바꾼 뒤 '순서 저장'을 눌러 주세요.");
    refreshControls();
    cancelButton.focus();
  });

  list.addEventListener("click", (event) => {
    const button = event.target.closest("[data-order-move]");
    if (!button || !editing || saving || button.disabled) return;
    const row = button.closest("[data-order-item-id]");
    const neighbor = button.dataset.orderMove === "up" ? row.previousElementSibling : row.nextElementSibling;
    if (!neighbor) return;
    if (button.dataset.orderMove === "up") list.insertBefore(row, neighbor);
    else list.insertBefore(neighbor, row);
    rows().forEach((item, index) => { item.querySelector(".col-order").textContent = index + 1; });
    showStatus(changed() ? "아직 저장하지 않은 순서입니다." : "원래 순서입니다.");
    refreshControls();
  });

  // Codex 작성: 취소하면 DOM 위치와 기존 표시 번호까지 복원합니다. 서버 요청은 보내지 않습니다.
  cancelButton.addEventListener("click", () => {
    if (saving) return;
    originalRows.forEach((row, index) => {
      list.append(row);
      row.querySelector(".col-order").textContent = originalNumbers[index];
    });
    editing = false;
    showStatus("순서 편집을 취소했습니다.");
    refreshControls();
    editButton.focus();
  });

  saveButton.addEventListener("click", async () => {
    if (!editing || saving || !changed()) return;
    saving = true;
    showStatus("순서를 저장하고 있습니다.");
    refreshControls();
    try {
      const response = await fetch(section.dataset.orderUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "same-origin",
        body: JSON.stringify({ originalItemIds: originalIds(), itemIds: ids() }),
      });
      if (response.status !== 204 || response.redirected) {
        const messages = {
          401: "로그인이 만료되었습니다. 다시 로그인한 뒤 저장해 주세요.",
          403: "이 계획의 순서를 변경할 권한이 없습니다.",
          404: "여행계획을 찾을 수 없습니다.",
          409: "다른 곳에서 일정이 변경되었습니다. 새로고침 후 다시 편집해 주세요.",
        };
        throw new Error(messages[response.status] || "순서를 저장하지 못했습니다. 다시 시도해 주세요.");
      }
      editing = false;
      showStatus("일정 순서를 저장했습니다.");
    } catch (error) {
      // Codex 작성: 실패 시 편집한 순서를 유지하여 다시 저장하거나 취소할 수 있게 합니다.
      showStatus(error instanceof TypeError ? "연결을 확인한 뒤 다시 저장해 주세요." : error.message, true);
    } finally {
      saving = false;
      refreshControls();
      if (!editing) editButton.focus();
    }
  });

  // Codex 작성: 저장하지 않은 순서가 있으면 새로고침·창 닫기·다른 화면 이동 전에 브라우저가 확인합니다.
  window.addEventListener("beforeunload", (event) => {
    if (editing && (changed() || saving)) {
      event.preventDefault();
      event.returnValue = "";
    }
  });
  refreshControls();
})();

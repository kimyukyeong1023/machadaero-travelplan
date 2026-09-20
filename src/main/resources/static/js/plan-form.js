// 생성·수정 화면에서 종료일이 시작일보다 빠르면 제출을 막습니다.
const startDate = document.getElementById("startDate");
const endDate = document.getElementById("endDate");

function validatePlanDates() {
  const invalid = startDate.value && endDate.value && endDate.value < startDate.value;
  endDate.setCustomValidity(invalid ? "종료일은 시작일보다 빠를 수 없습니다." : "");
}

startDate.addEventListener("input", validatePlanDates);
endDate.addEventListener("input", validatePlanDates);
validatePlanDates();

const API_ROOT = `${window.location.origin}/api`;
const API_BASE_URL = `${API_ROOT}/inventory`;
const PAGE_SIZE = 10;
const EXPIRING_WINDOW_DAYS = 30;
const state = {
  items: [],
  currentPage: 0,
  totalPages: 1,
  totalItems: 0,
  inventorySize: 0,
  allItems: [],
  loadSequence: 0,
  searchTimer: null,
  toastTimer: null,
  view: "dashboard",
  movementType: "IN",
  csrfToken: "",
  user: null
};

const elements = {
  rows: document.getElementById("inventoryRows"),
  historyRows: document.getElementById("historyRows"),
  historyCount: document.getElementById("historyCount"),
  messages: document.getElementById("contactMessages"),
  messagesCount: document.getElementById("messagesCount"),
  categoryChart: document.getElementById("categoryChart"),
  count: document.getElementById("inventoryCount"),
  search: document.getElementById("searchInput"),
  sort: document.getElementById("sortSelect"),
  category: document.getElementById("categorySelect"),
  lowStock: document.getElementById("lowStockToggle"),
  pageSummary: document.getElementById("pageSummary"),
  itemDialog: document.getElementById("itemDialog"),
  itemForm: document.getElementById("itemForm"),
  movementDialog: document.getElementById("movementDialog"),
  movementForm: document.getElementById("movementForm"),
  toast: document.getElementById("toast")
};

function itemName(item) {
  return item.name || item.productName || "Unnamed item";
}

function formatCurrency(value) {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  }).format(Number(value) || 0);
}

function formatDate(value) {
  if (!value) return "—";
  const date = new Date(`${value}T00:00:00`);
  if (Number.isNaN(date.getTime())) return "—";
  return new Intl.DateTimeFormat("en-IN", { day: "numeric", month: "short", year: "numeric" }).format(date);
}

function formatDateTime(value) {
  if (!value) return "—";
  const normalized = value.includes("T") ? value : value.replace(" ", "T");
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit"
  }).format(date);
}

function daysUntilExpiration(item) {
  if (!item.expirationDate) return null;
  const date = new Date(`${item.expirationDate}T00:00:00`);
  if (Number.isNaN(date.getTime())) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.ceil((date.getTime() - today.getTime()) / 86400000);
}

function showToast(message, isError = false) {
  window.clearTimeout(state.toastTimer);
  elements.toast.textContent = message;
  elements.toast.classList.toggle("error", isError);
  elements.toast.classList.add("show");
  state.toastTimer = window.setTimeout(() => elements.toast.classList.remove("show"), 3600);
}

async function requestJson(url, options) {
  const headers = new Headers(options?.headers || {});
  const method = (options?.method || "GET").toUpperCase();
  if (method !== "GET" && method !== "HEAD" && state.csrfToken) {
    headers.set("X-XSRF-TOKEN", state.csrfToken);
  }
  const response = await fetch(url, {
    ...options,
    headers,
    credentials: "same-origin"
  });
  let data;
  try {
    data = await response.json();
  } catch {
    throw new Error(`The server returned an invalid response (${response.status}).`);
  }
  if (!response.ok || data.success === false) {
    const error = new Error(data.message || `Request failed (${response.status}).`);
    error.status = response.status;
    throw error;
  }
  return data;
}

function updateStats(stats) {
  document.getElementById("statItems").textContent = Number(stats.totalItems || 0).toLocaleString("en-IN");
  document.getElementById("statQuantity").textContent = Number(stats.totalQuantity || 0).toLocaleString("en-IN");
  document.getElementById("statCategories").textContent = Number(stats.categoriesCount || 0).toLocaleString("en-IN");
  document.getElementById("statValue").textContent = formatCurrency(stats.totalStockValue);
  document.getElementById("statLowStock").textContent = Number(stats.lowStockCount || 0).toLocaleString("en-IN");
  document.getElementById("statExpiring").textContent = Number(stats.expiringCount || 0).toLocaleString("en-IN");
  renderCategoryChart(stats.categoryStock || {});
}

function renderCategoryChart(categoryStock) {
  const entries = Object.entries(categoryStock).sort((a, b) => b[1] - a[1]);
  elements.categoryChart.replaceChildren();
  if (!entries.length) {
    const message = document.createElement("p");
    message.className = "chart-message";
    message.textContent = "No category stock to display.";
    elements.categoryChart.append(message);
    return;
  }
  const maximum = Math.max(...entries.map(([, quantity]) => Number(quantity) || 0), 1);
  entries.forEach(([category, quantity]) => {
    const row = document.createElement("div");
    row.className = "chart-row";
    const label = document.createElement("span");
    label.className = "chart-category";
    label.textContent = category;
    const track = document.createElement("div");
    track.className = "chart-track";
    track.setAttribute("role", "img");
    track.setAttribute("aria-label", `${category}: ${quantity} units`);
    const bar = document.createElement("div");
    bar.className = "chart-bar";
    bar.style.width = `${Math.max(quantity > 0 ? 2 : 0, Number(quantity) / maximum * 100)}%`;
    track.append(bar);
    const value = document.createElement("strong");
    value.className = "chart-value";
    value.textContent = Number(quantity).toLocaleString("en-IN");
    row.append(label, track, value);
    elements.categoryChart.append(row);
  });
}

function updateCategoryOptions(categories) {
  const selected = elements.category.value;
  const options = [new Option("All categories", "")];
  [...categories].sort((a, b) => a.localeCompare(b)).forEach(category => {
    options.push(new Option(category, category));
  });
  elements.category.replaceChildren(...options);
  if (categories.includes(selected)) elements.category.value = selected;
}

function makeBadge(label, className) {
  const badge = document.createElement("span");
  badge.className = `badge ${className}`;
  badge.textContent = label;
  return badge;
}

function makeCell(row, text, className = "") {
  const cell = document.createElement("td");
  cell.textContent = text;
  if (className) cell.className = className;
  row.append(cell);
  return cell;
}

function makeAction(label, action, accessibleLabel, className = "") {
  const button = document.createElement("button");
  button.type = "button";
  button.className = `row-action${className ? ` ${className}` : ""}`;
  button.textContent = label;
  button.setAttribute("aria-label", accessibleLabel);
  button.addEventListener("click", action);
  return button;
}

function renderEmptyState() {
  const row = document.createElement("tr");
  const cell = document.createElement("td");
  cell.colSpan = 8;
  cell.className = "table-message";
  const box = document.createElement("div");
  box.className = "empty-state-box";
  const mark = document.createElement("div");
  mark.className = "empty-mark";
  mark.textContent = "—";
  const title = document.createElement("h3");
  title.textContent = state.inventorySize === 0 ? "No inventory yet" : "No matching items";
  const description = document.createElement("p");
  description.textContent = state.inventorySize === 0
    ? "Add your first product to start tracking stock and inventory value."
    : "Try changing your search or filters to find an item.";
  box.append(mark, title, description);
  if (state.inventorySize === 0) {
    const addButton = document.createElement("button");
    addButton.type = "button";
    addButton.className = "button button-primary";
    addButton.textContent = "Add first item";
    addButton.addEventListener("click", () => openItemDialog());
    box.append(addButton);
  }
  cell.append(box);
  row.append(cell);
  elements.rows.append(row);
}

function renderInventory() {
  elements.rows.replaceChildren();
  elements.rows.closest("table").setAttribute("aria-busy", "false");
  elements.count.textContent = `${state.totalItems.toLocaleString("en-IN")} ${state.totalItems === 1 ? "item" : "items"} found`;
  elements.pageSummary.textContent = `Page ${state.currentPage + 1} of ${state.totalPages}`;
  document.getElementById("previousPage").disabled = state.currentPage <= 0;
  document.getElementById("nextPage").disabled = state.currentPage >= state.totalPages - 1;
  if (!state.items.length) {
    renderEmptyState();
    return;
  }

  const maximum = Math.max(...state.items.map(item => Number(item.quantity) || 0), 1);
  state.items.forEach(item => {
    const row = document.createElement("tr");
    makeCell(row, item.productId || "—", "product-id-cell");
    const productCell = document.createElement("td");
    const product = document.createElement("div");
    product.className = "product-cell";
    const avatar = document.createElement("span");
    avatar.className = "product-avatar";
    avatar.textContent = itemName(item).slice(0, 2);
    const details = document.createElement("div");
    const name = document.createElement("div");
    name.className = "product-name";
    name.textContent = itemName(item);
    details.append(name);
    product.append(avatar, details);
    productCell.append(product);
    row.append(productCell);

    const quantity = Number(item.quantity) || 0;
    const reorderLevel = Number(item.reorderLevel ?? 10);
    const quantityCell = makeCell(row, "", "quantity-cell");
    const quantityText = document.createElement("div");
    quantityText.className = "quantity-text";
    const amount = document.createElement("strong");
    amount.textContent = quantity.toLocaleString("en-IN");
    quantityText.append(amount);
    const track = document.createElement("div");
    track.className = "quantity-track";
    track.setAttribute("role", "img");
    track.setAttribute("aria-label", `${quantity} units in stock`);
    const fill = document.createElement("div");
    fill.className = `quantity-fill${quantity <= reorderLevel ? " low" : ""}`;
    fill.style.width = `${Math.max(quantity > 0 ? 2 : 0, Math.min(100, quantity / maximum * 100))}%`;
    track.append(fill);
    quantityCell.append(quantityText, track);

    makeCell(row, item.category || "Uncategorized");
    makeCell(row, formatCurrency(item.unitPrice));
    makeCell(row, formatDate(item.expirationDate));

    const statusCell = document.createElement("td");
    const days = daysUntilExpiration(item);
    if (days !== null && days <= EXPIRING_WINDOW_DAYS) statusCell.append(makeBadge("Expiring soon", "expiring"));
    else if (quantity <= reorderLevel) statusCell.append(makeBadge("Low stock", "low"));
    else statusCell.append(makeBadge("In stock", "stock"));
    row.append(statusCell);

    const actionsCell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";
    actions.append(
      makeAction("Edit", () => openItemDialog(item), `Edit ${itemName(item)}`)
    );
    if (state.user?.role === "ADMIN") {
      actions.append(makeAction("Delete", () => deleteItem(item), `Delete ${itemName(item)}`, "delete"));
    }
    actionsCell.append(actions);
    row.append(actionsCell);
    elements.rows.append(row);
  });
}

function inventoryUrl(page = state.currentPage) {
  const sortValues = {
    name: ["name", "asc"],
    idAsc: ["productId", "asc"],
    quantityAsc: ["quantity", "asc"],
    quantityDesc: ["quantity", "desc"],
    expiration: ["expirationDate", "asc"],
    priceDesc: ["unitPrice", "desc"],
    category: ["category", "asc"]
  };
  const [sort, direction] = sortValues[elements.sort.value] || sortValues.name;
  const params = new URLSearchParams({
    page: String(page),
    size: String(PAGE_SIZE),
    sort,
    direction,
    search: elements.search.value.trim(),
    category: elements.category.value,
    lowStockOnly: String(elements.lowStock.checked)
  });
  return `${API_BASE_URL}/all?${params}`;
}

async function loadInventory() {
  const sequence = ++state.loadSequence;
  elements.count.textContent = "Loading inventory...";
  elements.rows.closest("table").setAttribute("aria-busy", "true");
  const loadingRow = document.createElement("tr");
  const loadingCell = document.createElement("td");
  loadingCell.colSpan = 8;
  loadingCell.className = "table-message loading-message";
  loadingCell.textContent = "Loading inventory...";
  loadingRow.append(loadingCell);
  elements.rows.replaceChildren(loadingRow);
  try {
    const [statsData, pageData] = await Promise.all([
      requestJson(`${API_BASE_URL}/statistics`),
      requestJson(inventoryUrl())
    ]);
    if (sequence !== state.loadSequence) return;
    if (!statsData.statistics || !Array.isArray(pageData.items)) {
      throw new Error("The server returned an incomplete inventory response.");
    }
    updateStats(statsData.statistics);
    state.inventorySize = Number(statsData.statistics.totalItems || 0);
    const oldCategory = elements.category.value;
    updateCategoryOptions(statsData.statistics.categories || []);
    if (oldCategory && elements.category.value !== oldCategory) {
      state.currentPage = 0;
      return loadInventory();
    }
    state.totalItems = Number(pageData.totalItems || 0);
    if (state.totalItems === 0 && state.currentPage !== 0) {
      state.currentPage = 0;
      return loadInventory();
    }
    if (pageData.totalPages > 0 && state.currentPage >= pageData.totalPages) {
      state.currentPage = pageData.totalPages - 1;
      return loadInventory();
    }
    state.items = pageData.items;
    state.currentPage = Number(pageData.page || 0);
    state.totalPages = Math.max(Number(pageData.totalPages || 0), 1);
    renderInventory();
  } catch (error) {
    if (sequence !== state.loadSequence) return;
    state.items = [];
    elements.rows.replaceChildren();
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 8;
    cell.className = "table-message";
    cell.textContent = `Unable to load inventory: ${error.message}`;
    row.append(cell);
    elements.rows.append(row);
    elements.count.textContent = "Inventory unavailable";
    elements.rows.closest("table").setAttribute("aria-busy", "false");
    showToast(`Unable to load inventory: ${error.message}`, true);
  }
}

function clearFormErrors(form) {
  form.querySelectorAll(".field").forEach(field => field.classList.remove("has-error"));
  form.querySelectorAll(".field-error").forEach(error => { error.textContent = ""; });
  const formError = document.getElementById(form.id === "itemForm" ? "formError" : "movementFormError");
  formError.hidden = true;
  formError.textContent = "";
}

function showFieldError(form, fieldName, message, prefix = "error-") {
  const error = document.getElementById(`${prefix}${fieldName}`);
  if (!error) return false;
  error.textContent = message;
  const field = error.closest(".field");
  if (field) field.classList.add("has-error");
  return true;
}

function displayFormError(form, message, prefix = "error-") {
  const normalized = message.toLowerCase();
  const candidates = [
    ["productId", /product.?id|identifier/],
    ["name", /product.?name|name cannot|name is required/],
    ["quantity", /quantity|stock/],
    ["category", /categor/],
    ["unitPrice", /price/],
    ["expirationDate", /expir/],
    ["reorderLevel", /reorder/]
  ];
  const match = candidates.find(([, pattern]) => pattern.test(normalized));
  if (match && showFieldError(form, match[0], message, prefix)) return;
  const formError = document.getElementById(form.id === "itemForm" ? "formError" : "movementFormError");
  formError.textContent = message;
  formError.hidden = false;
}

function validateItemForm(form, mode) {
  clearFormErrors(form);
  let valid = true;
  const fields = [
    ["productId", document.getElementById("productId"), "Product ID is required."],
    ["unitPrice", document.getElementById("unitPrice"), "Enter a valid non-negative unit price."],
    ["reorderLevel", document.getElementById("reorderLevel"), "Enter a non-negative reorder level."]
  ];
  if (mode === "add") fields.push(["quantity", document.getElementById("quantity"), "Enter a non-negative opening quantity."]);
  fields.forEach(([name, input, message]) => {
    const value = input.value.trim();
    const number = Number(value);
    const numericInvalid = input.type === "number" && (
      !Number.isFinite(number)
      || number < Number(input.min)
      || (input.step === "1" && !Number.isInteger(number))
    );
    if ((input.required && !value) || numericInvalid) {
      showFieldError(form, name, message);
      valid = false;
    }
  });
  const expiration = document.getElementById("expirationDate");
  if (expiration.value) {
    const today = new Date();
    today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
    if (expiration.value < today.toISOString().slice(0, 10)) {
      showFieldError(form, "expirationDate", "Expiration date cannot be in the past.");
      valid = false;
    }
  }
  return valid;
}

function openItemDialog(item = null) {
  const form = elements.itemForm;
  clearFormErrors(form);
  form.reset();
  const editing = Boolean(item);
  document.getElementById("formMode").value = editing ? "edit" : "add";
  document.getElementById("itemDialogTitle").textContent = editing ? "Edit item" : "Add item";
  document.getElementById("itemDialogSubtitle").textContent = editing
    ? "Update product details. Stock quantity is managed separately."
    : "Enter the product details and opening quantity.";
  document.getElementById("saveItemButton").textContent = editing ? "Save changes" : "Add item";
  const productId = document.getElementById("productId");
  productId.disabled = editing;
  const quantityInput = document.getElementById("quantity");
  quantityInput.disabled = editing;
  const quantityLabel = document.querySelector("label[for='quantity']");
  quantityLabel.firstChild.textContent = editing ? "Current quantity " : "Opening quantity ";
  quantityLabel.querySelector("span").hidden = editing;
  if (item) {
    productId.value = item.productId;
    document.getElementById("productName").value = itemName(item);
    document.getElementById("productCategory").value = item.category || "";
    document.getElementById("unitPrice").value = Number(item.unitPrice) || 0;
    document.getElementById("expirationDate").value = item.expirationDate || "";
    document.getElementById("reorderLevel").value = Number(item.reorderLevel ?? 10);
    quantityInput.value = Number(item.quantity) || 0;
  }
  elements.itemDialog.showModal();
  (editing ? document.getElementById("productName") : productId).focus();
}

async function saveItem(event) {
  event.preventDefault();
  const form = elements.itemForm;
  const mode = document.getElementById("formMode").value;
  if (!validateItemForm(form, mode)) return;
  const item = {
    productId: document.getElementById("productId").value.trim(),
    name: document.getElementById("productName").value.trim(),
    quantity: Number(document.getElementById("quantity").value),
    expirationDate: document.getElementById("expirationDate").value || null,
    category: document.getElementById("productCategory").value.trim(),
    unitPrice: Number(document.getElementById("unitPrice").value),
    reorderLevel: Number(document.getElementById("reorderLevel").value)
  };
  const button = document.getElementById("saveItemButton");
  button.disabled = true;
  try {
    const url = mode === "add"
      ? `${API_BASE_URL}/add`
      : `${API_BASE_URL}/${encodeURIComponent(item.productId)}`;
    await requestJson(url, {
      method: mode === "add" ? "POST" : "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(item)
    });
    elements.itemDialog.close();
    state.currentPage = 0;
    await loadInventory();
    showToast(mode === "add" ? "Item added to inventory." : "Item details updated.");
  } catch (error) {
    displayFormError(form, error.message);
  } finally {
    button.disabled = false;
  }
}

async function deleteItem(item) {
  if (!window.confirm(`Delete ${itemName(item)} (${item.productId})? This cannot be undone.`)) return;
  try {
    await requestJson(`${API_BASE_URL}/${encodeURIComponent(item.productId)}`, { method: "DELETE" });
    await loadInventory();
    showToast("Item deleted. Its stock history is retained.");
  } catch (error) {
    showToast(`Unable to delete item: ${error.message}`, true);
  }
}

function populateMovementProducts() {
  const select = document.getElementById("movementProduct");
  const previous = select.value;
  const options = [new Option("Select a product", "")];
  [...state.allItems]
    .sort((a, b) => itemName(a).localeCompare(itemName(b)))
    .forEach(item => options.push(new Option(`${itemName(item)} (${item.productId})`, item.productId)));
  select.replaceChildren(...options);
  if (state.allItems.some(item => item.productId === previous)) select.value = previous;
}

async function loadAllItems() {
  const data = await requestJson(`${API_BASE_URL}/all`);
  if (!Array.isArray(data.items)) throw new Error("The server did not return the product list.");
  state.allItems = data.items;
  populateMovementProducts();
}

async function openMovementDialog(type) {
  const form = elements.movementForm;
  clearFormErrors(form);
  form.reset();
  try {
    await loadAllItems();
    if (!state.allItems.length) {
      showToast("Add an inventory item before recording a stock movement.", true);
      return;
    }
    state.movementType = type;
    document.getElementById("movementType").value = type;
    document.getElementById("movementDialogTitle").textContent = type === "IN" ? "Stock in" : "Stock out";
    document.getElementById("movementDialogSubtitle").textContent = type === "IN"
      ? "Record incoming units for a product."
      : "Record units issued from available stock.";
    document.getElementById("movementActor").textContent = state.user.username;
    elements.movementDialog.showModal();
    document.getElementById("movementProduct").focus();
  } catch (error) {
    showToast(`Unable to load products: ${error.message}`, true);
  }
}

async function saveMovement(event) {
  event.preventDefault();
  const form = elements.movementForm;
  clearFormErrors(form);
  const productId = document.getElementById("movementProduct").value;
  const quantityInput = document.getElementById("movementQuantity");
  const quantity = Number(quantityInput.value);
  let valid = true;
  if (!productId) {
    showFieldError(form, "productId", "Select a product.", "movement-error-");
    valid = false;
  }
  if (!Number.isInteger(quantity) || quantity < 1) {
    showFieldError(form, "quantity", "Quantity must be a whole number greater than zero.", "movement-error-");
    valid = false;
  }
  if (!valid) return;
  const button = document.getElementById("submitMovementButton");
  button.disabled = true;
  try {
    await requestJson(`${API_BASE_URL}/stock-${state.movementType.toLowerCase()}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ productId, quantity })
    });
    elements.movementDialog.close();
    await Promise.all([loadInventory(), loadAllItems()]);
    showToast(state.movementType === "IN" ? "Stock received." : "Stock issued.");
    if (state.view === "history") await loadHistory();
  } catch (error) {
    displayFormError(form, error.message, "movement-error-");
  } finally {
    button.disabled = false;
  }
}

async function loadHistory() {
  elements.historyCount.textContent = "Loading movement history...";
  elements.historyRows.replaceChildren();
  try {
    const data = await requestJson(`${API_BASE_URL}/movements`);
    if (!Array.isArray(data.movements)) throw new Error("The server did not return movement records.");
    elements.historyCount.textContent = `${data.movements.length.toLocaleString("en-IN")} recorded movements`;
    if (!data.movements.length) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 5;
      cell.className = "table-message";
      cell.textContent = "No stock movements have been recorded yet.";
      row.append(cell);
      elements.historyRows.append(row);
      return;
    }
    data.movements.forEach(movement => {
      const row = document.createElement("tr");
      makeCell(row, movement.productId || "—");
      const typeCell = makeCell(row, movement.type === "IN" ? "Stock in" : "Stock out");
      typeCell.className = movement.type === "IN" ? "movement-in" : "movement-out";
      makeCell(row, Number(movement.quantity || 0).toLocaleString("en-IN"));
      makeCell(row, movement.user || movement.performedBy || "system");
      makeCell(row, formatDateTime(movement.movementDate));
      elements.historyRows.append(row);
    });
  } catch (error) {
    elements.historyCount.textContent = "Movement history unavailable";
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 5;
    cell.className = "table-message";
    cell.textContent = `Unable to load movement history: ${error.message}`;
    row.append(cell);
    elements.historyRows.append(row);
  }
}

async function refreshCsrfToken() {
  const data = await requestJson(`${API_ROOT}/auth/csrf`);
  state.csrfToken = data.token;
}

function showLogin() {
  state.user = null;
  document.getElementById("appShell").hidden = true;
  document.getElementById("loginView").hidden = false;
  document.getElementById("publicContactView").hidden = true;
}

function showAuthenticated(user) {
  state.user = user;
  document.getElementById("appShell").hidden = false;
  document.getElementById("adminMessagesNav").hidden = user.role !== "ADMIN";
  document.getElementById("sessionLabel").textContent = `${user.username} · ${user.role}`;
  setView("dashboard");
  loadInventory();
}

async function initializeSession() {
  try {
    const session = await requestJson(`${API_ROOT}/auth/me`);
    await refreshCsrfToken();
    showAuthenticated(session);
  } catch (error) {
    if (error.status === 401) {
      window.location.replace("login.html");
      return;
    }
    showToast(`Unable to verify your session: ${error.message}`, true);
  }
}

async function signOut() {
  try {
    await requestJson(`${API_ROOT}/auth/logout`, { method: "POST" });
  } catch (error) {
    showToast(`Sign out failed: ${error.message}`, true);
    return;
  }
  window.location.replace("login.html");
}

function clearContactErrors(form, formErrorId, prefix) {
  form.querySelectorAll(".field").forEach(field => field.classList.remove("has-error"));
  form.querySelectorAll(".field-error").forEach(error => { error.textContent = ""; });
  const formError = document.getElementById(formErrorId);
  formError.hidden = true;
  formError.textContent = "";
}

async function submitContact(event, form, fields, formErrorId, prefix) {
  event.preventDefault();
  clearContactErrors(form, formErrorId, prefix);
  const values = Object.fromEntries(Object.entries(fields).map(([key, id]) => [key, document.getElementById(id).value.trim()]));
  const missingField = Object.entries(values).find(([, value]) => !value)?.[0];
  if (missingField) {
    showFieldError(form, missingField, `${missingField[0].toUpperCase()}${missingField.slice(1)} is required.`, prefix);
    return;
  }
  try {
    if (!state.csrfToken) await refreshCsrfToken();
    const result = await requestJson(`${API_ROOT}/contact`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(values)
    });
    form.reset();
    showToast(result.message || "Your message was sent.");
  } catch (error) {
    const normalized = error.message.toLowerCase();
    const fieldEntry = Object.entries(fields).find(([key]) => normalized.includes(key.toLowerCase()));
    if (fieldEntry && showFieldError(form, fieldEntry[0], error.message, prefix)) return;
    const formError = document.getElementById(formErrorId);
    formError.textContent = error.message;
    formError.hidden = false;
  }
}

async function loadContactMessages() {
  const container = elements.messages;
  container.replaceChildren();
  elements.messagesCount.textContent = "Loading contact messages...";
  try {
    const data = await requestJson(`${API_ROOT}/contact/messages`);
    elements.messagesCount.textContent = `${Number(data.count || 0).toLocaleString("en-IN")} messages`;
    if (!data.messages.length) {
      const empty = document.createElement("p");
      empty.className = "message-empty";
      empty.textContent = "No contact messages yet.";
      container.append(empty);
      return;
    }
    data.messages.forEach(message => {
      const article = document.createElement("article");
      article.className = "contact-message";
      const heading = document.createElement("div");
      heading.className = "contact-message-heading";
      const identity = document.createElement("div");
      const name = document.createElement("h2");
      name.textContent = message.name;
      const email = document.createElement("a");
      email.href = `mailto:${message.email}`;
      email.textContent = message.email;
      identity.append(name, email);
      const date = document.createElement("time");
      date.dateTime = message.createdAt;
      date.textContent = formatDateTime(message.createdAt);
      heading.append(identity, date);
      const body = document.createElement("p");
      body.textContent = message.message;
      article.append(heading, body);
      container.append(article);
    });
  } catch (error) {
    elements.messagesCount.textContent = "Contact messages unavailable";
    const message = document.createElement("p");
    message.className = "message-empty";
    message.textContent = error.message;
    container.append(message);
  }
}

function setView(view) {
  if (view === "messages" && state.user?.role !== "ADMIN") return;
  state.view = view;
  document.getElementById("dashboardView").hidden = !["dashboard", "inventory"].includes(view);
  document.getElementById("historyView").hidden = view !== "history";
  document.getElementById("contactView").hidden = view !== "contact";
  document.getElementById("messagesView").hidden = view !== "messages";
  document.querySelectorAll(".nav-link[data-view]").forEach(button => {
    button.classList.toggle("active", button.dataset.view === view);
  });
  if (view === "history") loadHistory();
  else if (view === "messages" && state.user?.role === "ADMIN") loadContactMessages();
  else if (view === "inventory") document.getElementById("inventorySection").scrollIntoView({ block: "start" });
  else window.scrollTo({ top: 0, behavior: "smooth" });
}

function refreshOnFilterChange() {
  state.currentPage = 0;
  loadInventory();
}

document.querySelectorAll(".nav-link[data-view]").forEach(button => {
  button.addEventListener("click", () => setView(button.dataset.view));
});
document.querySelectorAll(".nav-link[data-movement]").forEach(button => {
  button.addEventListener("click", () => openMovementDialog(button.dataset.movement));
});
document.getElementById("addItemButton").addEventListener("click", () => openItemDialog());
document.getElementById("refreshHistoryButton").addEventListener("click", loadHistory);
document.getElementById("refreshMessagesButton").addEventListener("click", loadContactMessages);
document.getElementById("logoutButton").addEventListener("click", signOut);
document.getElementById("contactForm").addEventListener("submit", event => submitContact(
  event,
  document.getElementById("contactForm"),
  { name: "contactName", email: "contactEmail", message: "contactMessage" },
  "contactFormError",
  "contact-error-"
));
document.getElementById("previousPage").addEventListener("click", () => {
  if (state.currentPage > 0) {
    state.currentPage -= 1;
    loadInventory();
  }
});
document.getElementById("nextPage").addEventListener("click", () => {
  if (state.currentPage < state.totalPages - 1) {
    state.currentPage += 1;
    loadInventory();
  }
});
elements.search.addEventListener("input", () => {
  window.clearTimeout(state.searchTimer);
  state.searchTimer = window.setTimeout(refreshOnFilterChange, 250);
});
elements.sort.addEventListener("change", refreshOnFilterChange);
elements.category.addEventListener("change", refreshOnFilterChange);
elements.lowStock.addEventListener("change", refreshOnFilterChange);
elements.itemForm.addEventListener("submit", saveItem);
elements.movementForm.addEventListener("submit", saveMovement);
document.querySelectorAll("[data-close-dialog]").forEach(button => {
  button.addEventListener("click", () => document.getElementById(button.dataset.closeDialog).close());
});
document.querySelectorAll("dialog").forEach(dialog => {
  dialog.addEventListener("click", event => {
    if (event.target === dialog) dialog.close();
  });
});

document.getElementById("todayLabel").textContent = new Intl.DateTimeFormat("en-IN", {
  weekday: "short", day: "numeric", month: "short", year: "numeric"
}).format(new Date());

initializeSession();

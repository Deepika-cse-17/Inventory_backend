const API_ROOT = `${window.location.origin}/api`;
let csrfToken = "";

async function requestJson(url, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.method && options.method.toUpperCase() !== "GET" && csrfToken) {
    headers.set("X-XSRF-TOKEN", csrfToken);
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

async function initializeLogin() {
  try {
    await requestJson(`${API_ROOT}/auth/me`);
    window.location.replace("index.html");
    return;
  } catch (error) {
    if (error.status !== 401) {
      const message = document.getElementById("connectionError");
      message.textContent = `Unable to verify your session: ${error.message}`;
      message.hidden = false;
      return;
    }
  }

  try {
    const csrf = await requestJson(`${API_ROOT}/auth/csrf`);
    csrfToken = csrf.token;
  } catch (error) {
    const message = document.getElementById("connectionError");
    message.textContent = `Unable to connect to the service: ${error.message}`;
    message.hidden = false;
    document.getElementById("loginButton").disabled = true;
  }
}

function clearAuthErrors() {
  ["usernameError", "passwordError", "registerUsernameError", "registerPasswordError", "confirmPasswordError"]
    .forEach(id => {
      const error = document.getElementById(id);
      error.textContent = "";
      error.closest(".field").classList.remove("has-error");
    });
  ["loginError", "registerError"].forEach(id => {
    const error = document.getElementById(id);
    error.textContent = "";
    error.hidden = true;
  });
}

function showAuthFieldError(input, error, message) {
  error.textContent = message;
  input.closest(".field").classList.add("has-error");
}

document.getElementById("authModeButton").addEventListener("click", event => {
  const registering = document.getElementById("registerForm").hidden;
  document.getElementById("registerForm").hidden = !registering;
  document.getElementById("loginForm").hidden = registering;
  document.getElementById("loginTitle").textContent = registering ? "Create account" : "Sign in";
  document.getElementById("authSubtitle").textContent = registering
    ? "Create your personal account to access the inventory."
    : "Sign in to manage warehouse inventory.";
  event.currentTarget.textContent = registering ? "Already have an account? Sign in" : "Need an account? Create one";
  clearAuthErrors();
});

document.getElementById("loginForm").addEventListener("submit", async event => {
  event.preventDefault();
  const usernameInput = document.getElementById("username");
  const passwordInput = document.getElementById("password");
  const usernameError = document.getElementById("usernameError");
  const passwordError = document.getElementById("passwordError");
  const loginError = document.getElementById("loginError");
  const button = document.getElementById("loginButton");

  clearAuthErrors();

  if (!usernameInput.value.trim() || !passwordInput.value) {
    if (!usernameInput.value.trim()) {
      usernameError.textContent = "Username is required.";
      usernameError.closest(".field").classList.add("has-error");
    }
    if (!passwordInput.value) {
      passwordError.textContent = "Password is required.";
      passwordError.closest(".field").classList.add("has-error");
    }
    return;
  }

  button.disabled = true;
  try {
    const result = await requestJson(`${API_ROOT}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: usernameInput.value.trim(),
        password: passwordInput.value
      })
    });
    passwordInput.value = "";
    window.location.replace("index.html");
  } catch (error) {
    loginError.textContent = error.status === 401
      ? "Incorrect username or password. Try again."
      : error.message;
    loginError.hidden = false;
  } finally {
    button.disabled = false;
  }
});

document.getElementById("registerForm").addEventListener("submit", async event => {
  event.preventDefault();
  clearAuthErrors();
  const usernameInput = document.getElementById("registerUsername");
  const passwordInput = document.getElementById("registerPassword");
  const confirmInput = document.getElementById("confirmPassword");
  const usernameError = document.getElementById("registerUsernameError");
  const passwordError = document.getElementById("registerPasswordError");
  const confirmError = document.getElementById("confirmPasswordError");
  const registerError = document.getElementById("registerError");
  const button = document.getElementById("registerButton");
  const username = usernameInput.value.trim();
  let valid = true;

  if (!/^[A-Za-z0-9@._-]{3,50}$/.test(username)) {
    showAuthFieldError(usernameInput, usernameError,
      "Use 3–50 letters, numbers, @, dots, underscores, or hyphens.");
    valid = false;
  }
  if (passwordInput.value.length < 12 || new TextEncoder().encode(passwordInput.value).length > 72) {
    showAuthFieldError(passwordInput, passwordError,
      "Password must be at least 12 characters and no more than 72 UTF-8 bytes.");
    valid = false;
  }
  if (passwordInput.value !== confirmInput.value) {
    showAuthFieldError(confirmInput, confirmError, "Passwords do not match.");
    valid = false;
  }
  if (!valid) return;

  button.disabled = true;
  try {
    await requestJson(`${API_ROOT}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password: passwordInput.value })
    });
    passwordInput.value = "";
    confirmInput.value = "";
    window.location.replace("index.html");
  } catch (error) {
    registerError.textContent = error.message;
    registerError.hidden = false;
  } finally {
    button.disabled = false;
  }
});

initializeLogin();
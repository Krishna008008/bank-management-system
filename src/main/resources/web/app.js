/**
 * Bank Frontend Application Logic
 */

const API_BASE = '/api';
let currentUser = null;

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    const saved = sessionStorage.getItem('bank_user');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
            showDashboard();
            refreshAccountDetails();
            refreshStatement();
        } catch (e) {
            sessionStorage.removeItem('bank_user');
        }
    }

    document.getElementById('btnLogout').addEventListener('click', handleLogout);
});

// ==================== AUTH TABS ====================
function switchAuthTab(tab) {
    const tabLogin = document.getElementById('tabLogin');
    const tabSignup = document.getElementById('tabSignup');
    const loginForm = document.getElementById('loginForm');
    const signupForm = document.getElementById('signupForm');

    if (tab === 'login') {
        tabLogin.classList.add('active');
        tabSignup.classList.remove('active');
        loginForm.classList.remove('hidden');
        signupForm.classList.add('hidden');
    } else {
        tabSignup.classList.add('active');
        tabLogin.classList.remove('active');
        signupForm.classList.remove('hidden');
        loginForm.classList.add('hidden');
    }
}

function handleAccountTypeChange() {
    const accType = document.getElementById('signupAccountType').value;
    const depositInput = document.getElementById('signupInitialDeposit');
    if (accType === 'Savings') {
        depositInput.min = '100';
        depositInput.placeholder = '100.00';
    } else {
        depositInput.min = '0';
        depositInput.placeholder = '0.00';
    }
}

// ==================== AUTH HANDLERS ====================
async function handleLogin(e) {
    e.preventDefault();
    const phone = document.getElementById('loginPhone').value.trim();
    const password = document.getElementById('loginPin').value.trim();
    const btn = document.getElementById('btnLoginSubmit');

    btn.disabled = true;
    btn.innerHTML = '<span>Verifying...</span>';

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ phoneNumber: phone, password: password })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            currentUser = data;
            sessionStorage.setItem('bank_user', JSON.stringify(currentUser));
            showToast('Login successful! Welcome back.', 'success');
            showDashboard();
            refreshStatement();
        } else {
            showToast(data.error || 'Invalid credentials', 'error');
        }
    } catch (err) {
        showToast('Network error connecting to API', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>Secure Login</span>';
    }
}

async function handleSignup(e) {
    e.preventDefault();
    const firstName = document.getElementById('signupFirstName').value.trim();
    const lastName = document.getElementById('signupLastName').value.trim();
    const phone = document.getElementById('signupPhone').value.trim();
    const pin = document.getElementById('signupPin').value.trim();
    const confirmPin = document.getElementById('signupConfirmPin').value.trim();
    const accountType = document.getElementById('signupAccountType').value;
    const initialDeposit = parseFloat(document.getElementById('signupInitialDeposit').value);
    const btn = document.getElementById('btnSignupSubmit');

    if (pin !== confirmPin) {
        showToast('PINs do not match. Please verify.', 'error');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span>Creating Account...</span>';

    try {
        const res = await fetch(`${API_BASE}/accounts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                firstName,
                lastName,
                phoneNumber: phone,
                password: pin,
                accountType,
                initialBalance: initialDeposit
            })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            currentUser = data;
            sessionStorage.setItem('bank_user', JSON.stringify(currentUser));
            showToast(`Account ${data.accountNumber} created successfully!`, 'success');
            showDashboard();
            refreshStatement();
        } else {
            showToast(data.error || 'Account creation failed', 'error');
        }
    } catch (err) {
        showToast('Network error connecting to API', 'error');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<span>Create Account</span>';
    }
}

function handleLogout() {
    currentUser = null;
    sessionStorage.removeItem('bank_user');
    document.getElementById('loginForm').reset();
    document.getElementById('signupForm').reset();
    showAuth();
    showToast('Logged out securely', 'success');
}

// ==================== DASHBOARD & TRANSACTIONS ====================
function showDashboard() {
    document.getElementById('authSection').classList.add('hidden');
    document.getElementById('dashboardSection').classList.remove('hidden');
    document.getElementById('navUserSection').classList.remove('hidden');

    document.getElementById('navUserName').textContent = currentUser.accountHolderName;
    document.getElementById('dashHolderName').textContent = currentUser.accountHolderName;
    document.getElementById('dashAccountNumber').textContent = currentUser.accountNumber;
    document.getElementById('dashPhoneNumber').textContent = currentUser.phoneNumber;
    document.getElementById('dashBalance').textContent = formatCurrency(currentUser.balance);
    document.getElementById('dashAccountType').textContent = currentUser.accountType || 'Banking';
}

function showAuth() {
    document.getElementById('dashboardSection').classList.add('hidden');
    document.getElementById('navUserSection').classList.add('hidden');
    document.getElementById('authSection').classList.remove('hidden');
    switchAuthTab('login');
}

async function refreshAccountDetails() {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/accounts/${currentUser.accountNumber}`);
        const data = await res.json();
        if (res.ok && data.success) {
            currentUser.balance = data.balance;
            currentUser.accountHolderName = data.accountHolderName;
            document.getElementById('dashBalance').textContent = formatCurrency(data.balance);
            sessionStorage.setItem('bank_user', JSON.stringify(currentUser));
        }
    } catch (e) {}
}

async function refreshStatement() {
    if (!currentUser) return;
    const tbody = document.getElementById('statementTableBody');
    try {
        const res = await fetch(`${API_BASE}/accounts/${currentUser.accountNumber}/statement`);
        const data = await res.json();
        if (res.ok && data.success && data.transactions) {
            tbody.innerHTML = '';
            if (data.transactions.length === 0) {
                tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">No transactions recorded yet.</td></tr>`;
                return;
            }

            // Loop in reverse (newest first)
            for (let i = data.transactions.length - 1; i >= 0; i--) {
                const item = data.transactions[i]; // e.g. "2026-09-21 10:50:00 - Deposit: $100.0"
                const parts = item.split(' - ');
                const timestamp = parts[0] || '';
                const desc = parts.slice(1).join(' - ') || item;

                let badgeHtml = '<span class="badge badge-neutral">System</span>';
                if (desc.toLowerCase().includes('deposit') || desc.toLowerCase().includes('initial deposit') || desc.toLowerCase().includes('transfer from')) {
                    badgeHtml = '<span class="badge badge-credit">+ Credit</span>';
                } else if (desc.toLowerCase().includes('withdraw') || desc.toLowerCase().includes('transfer to')) {
                    badgeHtml = '<span class="badge badge-debit">- Debit</span>';
                }

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td class="font-mono">${timestamp}</td>
                    <td>${desc}</td>
                    <td>${badgeHtml}</td>
                `;
                tbody.appendChild(tr);
            }
        }
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="3" class="text-center text-muted">Unable to load statement ledger.</td></tr>`;
    }
}

async function handleDeposit(e) {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('depAmount').value);
    const pin = document.getElementById('depPin').value.trim();

    try {
        const res = await fetch(`${API_BASE}/transactions/deposit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                accountNumber: currentUser.accountNumber,
                amount: amount,
                pin: pin
            })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(data.message, 'success');
            document.getElementById('depositForm').reset();
            await refreshAccountDetails();
            await refreshStatement();
        } else {
            showToast(data.error || 'Deposit failed', 'error');
        }
    } catch (err) {
        showToast('Network error during deposit', 'error');
    }
}

async function handleWithdraw(e) {
    e.preventDefault();
    const amount = parseFloat(document.getElementById('withAmount').value);
    const pin = document.getElementById('withPin').value.trim();

    try {
        const res = await fetch(`${API_BASE}/transactions/withdraw`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                accountNumber: currentUser.accountNumber,
                amount: amount,
                pin: pin
            })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(data.message, 'success');
            document.getElementById('withdrawForm').reset();
            await refreshAccountDetails();
            await refreshStatement();
        } else {
            showToast(data.error || 'Withdrawal failed', 'error');
        }
    } catch (err) {
        showToast('Network error during withdrawal', 'error');
    }
}

async function handleTransfer(e) {
    e.preventDefault();
    const toAccount = document.getElementById('destAccount').value.trim();
    const amount = parseFloat(document.getElementById('transferAmount').value);
    const pin = document.getElementById('transferPin').value.trim();

    try {
        const res = await fetch(`${API_BASE}/transactions/transfer`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fromAccount: currentUser.accountNumber,
                toAccount: toAccount,
                amount: amount,
                pin: pin
            })
        });

        const data = await res.json();
        if (res.ok && data.success) {
            showToast(data.message, 'success');
            document.getElementById('transferForm').reset();
            await refreshAccountDetails();
            await refreshStatement();
        } else {
            showToast(data.error || 'Transfer failed', 'error');
        }
    } catch (err) {
        showToast('Network error during transfer', 'error');
    }
}

// ==================== HELPERS ====================
function formatCurrency(val) {
    return Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function showToast(message, type = 'success') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <span>${type === 'success' ? '✓' : '✕'}</span>
        <div>${message}</div>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

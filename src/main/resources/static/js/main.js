// ============================================================
// Student Management System - Main Interactive Script
// ============================================================

document.addEventListener('DOMContentLoaded', function () {
    // Mobile sidebar toggle
    const toggleBtn = document.getElementById('sidebarToggle');
    const sidebar = document.querySelector('.sidebar');
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', function () {
            sidebar.classList.toggle('d-none');
        });
    }

    // Auto-dismiss alerts after 6 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 6000);
    });
});

// Utility Toast Notification
function showToast(message, isError = false) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white ${isError ? 'bg-danger' : 'bg-success'} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body d-flex align-items-center gap-2">
                <i class="bi ${isError ? 'bi-exclamation-triangle-fill' : 'bi-check-circle-fill'}"></i>
                <span>${message}</span>
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    container.appendChild(toastEl);
    const bsToast = new bootstrap.Toast(toastEl, { delay: 4000 });
    bsToast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

// Admin Student Status Toggle
async function toggleStudentStatus(studentId, activate) {
    if (!confirm(`Are you sure you want to ${activate ? 'activate' : 'disable'} this student account?`)) {
        return;
    }
    try {
        const response = await fetch(`/api/admin/students/${studentId}/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ activate: activate })
        });
        const result = await response.json();
        if (response.ok) {
            showToast(result.message || 'Status updated successfully');
            setTimeout(() => window.location.reload(), 800);
        } else {
            showToast(result.message || 'Failed to update status', true);
        }
    } catch (err) {
        showToast('Network error while updating status', true);
    }
}

// Resend Verification Email
async function resendVerification(studentId) {
    try {
        const response = await fetch(`/api/admin/students/${studentId}/resend-verification`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        const result = await response.json();
        if (response.ok) {
            showToast(result.message || 'Verification email dispatched');
        } else {
            showToast(result.message || 'Failed to dispatch verification email', true);
        }
    } catch (err) {
        showToast('Network error while dispatching email', true);
    }
}

// Delete Entity Handler
async function deleteEntity(endpoint, entityName) {
    if (!confirm(`Are you sure you want to permanently delete this ${entityName}? This action cannot be undone.`)) {
        return;
    }
    try {
        const response = await fetch(endpoint, {
            method: 'DELETE'
        });
        const result = await response.json();
        if (response.ok) {
            showToast(result.message || `${entityName} deleted successfully`);
            setTimeout(() => window.location.reload(), 700);
        } else {
            showToast(result.message || `Failed to delete ${entityName}`, true);
        }
    } catch (err) {
        showToast('Network error while deleting item', true);
    }
}

// Print Handler
function printReport() {
    window.print();
}

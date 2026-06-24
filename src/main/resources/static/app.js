(() => {
  const appRoot = document.getElementById('app-root');
  const topbar = document.getElementById('topbar');
  const breadcrumbsEl = document.getElementById('breadcrumbs');
  const modalRoot = document.getElementById('modal-root');
  const toastRoot = document.getElementById('toast-root');
  const logoutBtn = document.getElementById('logout-btn');
  const themeToggleBtn = document.getElementById('theme-toggle');
  const profileChip = document.getElementById('profile-chip');
  const profileMenu = document.getElementById('profile-menu');
  const profileAvatar = document.getElementById('profile-avatar');
  const profileName = document.getElementById('profile-name');

  // ---------- helpers ----------

  function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function getToken() { return localStorage.getItem('et_token'); }
  function getEmail() { return localStorage.getItem('et_email'); }
  function getUserId() {
    const token = getToken();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
      return json.sub || null;
    } catch (e) {
      return null;
    }
  }
  function setSession(token, email) {
    localStorage.setItem('et_token', token);
    localStorage.setItem('et_email', email);
  }
  function clearSession() {
    localStorage.removeItem('et_token');
    localStorage.removeItem('et_email');
  }

  function toast(message, type = 'error') {
    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.textContent = message;
    toastRoot.appendChild(el);
    setTimeout(() => el.remove(), 4000);
  }

  async function api(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const res = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (res.status === 401) {
      clearSession();
      if (location.hash !== '#/login') location.hash = '#/login';
      throw new Error('Session expired. Please log in again.');
    }

    const text = await res.text();
    let data = null;
    if (text) {
      try { data = JSON.parse(text); } catch (e) { data = null; }
    }

    if (!res.ok) {
      const message = (data && data.message) || `Request failed (${res.status})`;
      throw new Error(message);
    }
    return data;
  }

  function openModal(title, bodyHtml) {
    modalRoot.innerHTML = `
      <div class="modal-backdrop" id="modal-backdrop">
        <div class="modal" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h3>${escapeHtml(title)}</h3>
            <button class="modal-close" id="modal-close" aria-label="Close">&times;</button>
          </div>
          <div class="modal-body">${bodyHtml}</div>
        </div>
      </div>`;
    document.getElementById('modal-backdrop').addEventListener('click', (e) => {
      if (e.target.id === 'modal-backdrop') closeModal();
    });
    document.getElementById('modal-close').addEventListener('click', closeModal);
    return modalRoot.querySelector('.modal-body');
  }

  function closeModal() { modalRoot.innerHTML = ''; }

  function openPanel(title, bodyHtml) {
    modalRoot.innerHTML = `
      <div class="panel-backdrop" id="panel-backdrop">
        <aside class="side-panel" role="dialog" aria-modal="true">
          <div class="modal-header">
            <h3>${escapeHtml(title)}</h3>
            <button class="modal-close" id="panel-close" aria-label="Close">&times;</button>
          </div>
          <div class="modal-body">${bodyHtml}</div>
        </aside>
      </div>`;
    document.getElementById('panel-backdrop').addEventListener('click', (e) => {
      if (e.target.id === 'panel-backdrop') closePanel();
    });
    document.getElementById('panel-close').addEventListener('click', closePanel);
    const panel = modalRoot.querySelector('.side-panel');
    requestAnimationFrame(() => panel.classList.add('open'));
    return modalRoot.querySelector('.modal-body');
  }

  function closePanel() {
    const backdrop = document.getElementById('panel-backdrop');
    if (!backdrop) return;
    const panel = backdrop.querySelector('.side-panel');
    panel.classList.remove('open');
    setTimeout(() => { if (modalRoot.contains(backdrop)) modalRoot.innerHTML = ''; }, 220);
  }

  function setBreadcrumbs(parts) {
    breadcrumbsEl.innerHTML = parts.map((p, i) => {
      const isLast = i === parts.length - 1;
      const label = escapeHtml(p.label);
      if (isLast || !p.hash) {
        return `<span class="crumb-current">${label}</span>`;
      }
      return `<a href="#${p.hash}">${label}</a><span class="crumb-sep">/</span>`;
    }).join('');
  }

  function formatDate(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  function roleBadge(role) {
    if (!role) return '';
    const cls = `badge badge-role-${role.toLowerCase()}`;
    return `<span class="${cls}">${escapeHtml(role)}</span>`;
  }

  function priorityBadge(priority) {
    const cls = `badge badge-priority-${priority.toLowerCase()}`;
    return `<span class="${cls}">${escapeHtml(priority)}</span>`;
  }

  function statusBadge(status) {
    if (!status || status === 'ACTIVE') return '';
    const cls = `badge badge-status-${status.toLowerCase()}`;
    return `<span class="${cls}">${escapeHtml(status)}</span>`;
  }

  function canManageWorkspace(workspace) {
    return workspace.myRole === 'OWNER' || workspace.myRole === 'ADMIN';
  }

  function canManageProject(workspace, project) {
    return canManageWorkspace(workspace) || project.myRole === 'LEAD';
  }

  function canWriteProject(workspace, project) {
    return workspace.myRole !== 'MEMBER' || (project.myRole && project.myRole !== 'VIEWER');
  }

  function initials(label) {
    if (!label) return '?';
    const parts = label.replace(/@.*$/, '').split(/[.\s_-]+/).filter(Boolean);
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }

  // ---------- theme ----------

  const THEME_KEY = 'et_theme';

  function effectiveTheme() {
    return localStorage.getItem(THEME_KEY) ||
      (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    themeToggleBtn.dataset.mode = theme;
    themeToggleBtn.setAttribute('aria-pressed', String(theme === 'dark'));
  }

  themeToggleBtn.addEventListener('click', () => {
    const next = effectiveTheme() === 'dark' ? 'light' : 'dark';
    localStorage.setItem(THEME_KEY, next);
    applyTheme(next);
  });

  applyTheme(effectiveTheme());

  // ---------- profile menu ----------

  profileChip.addEventListener('click', (e) => {
    e.stopPropagation();
    const isHidden = profileMenu.classList.contains('hidden');
    profileMenu.classList.toggle('hidden', !isHidden);
    profileChip.setAttribute('aria-expanded', String(isHidden));
  });
  document.addEventListener('click', () => {
    profileMenu.classList.add('hidden');
    profileChip.setAttribute('aria-expanded', 'false');
  });

  // ---------- router ----------

  function currentRoute() {
    const hash = location.hash.slice(1);
    if (!hash) return getToken() ? '/workspaces' : '/login';
    return hash;
  }

  const ROUTES = [
    { pattern: /^\/login$/, view: renderLogin },
    { pattern: /^\/register$/, view: renderRegister },
    { pattern: /^\/accept-invite\/([^/]+)$/, view: renderAcceptInvite },
    { pattern: /^\/workspaces$/, view: renderWorkspaces },
    { pattern: /^\/w\/([^/]+)$/, view: renderWorkspaceDetail },
    { pattern: /^\/w\/([^/]+)\/p\/([^/]+)$/, view: renderProjectDetail },
  ];

  async function route() {
    const path = currentRoute();
    const token = getToken();
    const isInviteRoute = path.startsWith('/accept-invite/');
    const isAuthRoute = path === '/login' || path === '/register' || isInviteRoute;

    if (!token && !isAuthRoute) {
      location.hash = '#/login';
      return;
    }
    if (token && isAuthRoute && !isInviteRoute) {
      location.hash = '#/workspaces';
      return;
    }

    topbar.classList.toggle('hidden', !token);
    if (token) {
      const email = getEmail() || '';
      profileAvatar.textContent = initials(email);
      profileName.textContent = email.replace(/@.*$/, '');
    }
    breadcrumbsEl.innerHTML = '';

    for (const r of ROUTES) {
      const match = path.match(r.pattern);
      if (match) {
        appRoot.innerHTML = '<div class="loading">Loading…</div>';
        try {
          await r.view(...match.slice(1));
        } catch (err) {
          toast(err.message);
          appRoot.innerHTML = `<div class="empty-state">${escapeHtml(err.message)}</div>`;
        }
        return;
      }
    }
    location.hash = token ? '#/workspaces' : '#/login';
  }

  window.addEventListener('hashchange', route);
  window.addEventListener('DOMContentLoaded', route);
  logoutBtn.addEventListener('click', () => {
    clearSession();
    location.hash = '#/login';
  });

  // ---------- auth views ----------

  function renderLogin() {
    appRoot.innerHTML = `
      <div class="auth-shell">
        <h1 class="auth-title">Welcome back</h1>
        <p class="auth-subtitle">Sign in to easyTask</p>
        <div class="tabs">
          <button class="tab active">Log in</button>
          <a class="tab" href="#/register" style="text-decoration:none;display:flex;align-items:center;">Register</a>
        </div>
        <form id="login-form">
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" required autocomplete="username">
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input id="password" type="password" required autocomplete="current-password">
          </div>
          <p class="error-text" id="form-error"></p>
          <button type="submit" class="btn btn-block">Log in</button>
        </form>
      </div>`;

    document.getElementById('login-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      const errorEl = document.getElementById('form-error');
      errorEl.textContent = '';
      try {
        const res = await api('POST', '/api/auth/login', { email, password });
        setSession(res.token, res.email);
        location.hash = '#/workspaces';
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  function renderRegister() {
    appRoot.innerHTML = `
      <div class="auth-shell">
        <h1 class="auth-title">Create your account</h1>
        <p class="auth-subtitle">Get started with easyTask</p>
        <div class="tabs">
          <a class="tab" href="#/login" style="text-decoration:none;display:flex;align-items:center;">Log in</a>
          <button class="tab active">Register</button>
        </div>
        <form id="register-form">
          <div class="field">
            <label for="displayName">Display name</label>
            <input id="displayName" type="text" required>
          </div>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" type="email" required autocomplete="username">
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input id="password" type="password" required minlength="8" autocomplete="new-password">
            <span class="field-hint">At least 8 characters.</span>
          </div>
          <p class="error-text" id="form-error"></p>
          <button type="submit" class="btn btn-block">Create account</button>
        </form>
      </div>`;

    document.getElementById('register-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = document.getElementById('displayName').value.trim();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      const errorEl = document.getElementById('form-error');
      errorEl.textContent = '';
      try {
        const res = await api('POST', '/api/auth/register', { email, password, displayName });
        setSession(res.token, res.email);
        location.hash = '#/workspaces';
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  function renderAcceptInvite(token) {
    appRoot.innerHTML = `
      <div class="auth-shell">
        <h1 class="auth-title">Accept invite</h1>
        <p class="auth-subtitle">Set a password to activate your account</p>
        <form id="accept-invite-form">
          <div class="field">
            <label for="displayName">Display name</label>
            <input id="displayName" type="text" maxlength="120" placeholder="Optional, leave blank to keep the current one">
          </div>
          <div class="field">
            <label for="password">Password</label>
            <input id="password" type="password" required minlength="8" autocomplete="new-password">
            <span class="field-hint">At least 8 characters.</span>
          </div>
          <p class="error-text" id="form-error"></p>
          <button type="submit" class="btn btn-block">Activate account</button>
        </form>
      </div>`;

    document.getElementById('accept-invite-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const displayName = document.getElementById('displayName').value.trim();
      const password = document.getElementById('password').value;
      const errorEl = document.getElementById('form-error');
      errorEl.textContent = '';
      try {
        const res = await api('POST', '/api/auth/accept-invite', { token, password, displayName: displayName || null });
        setSession(res.token, res.email);
        location.hash = '#/workspaces';
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  // ---------- workspaces ----------

  async function renderWorkspaces() {
    const workspaces = await api('GET', '/api/workspaces');
    appRoot.innerHTML = `
      <div class="page-header">
        <div>
          <h1>Workspaces</h1>
          <div class="subtitle">Pick a workspace to see its projects.</div>
        </div>
      </div>
      <div class="card-grid" id="workspace-grid"></div>`;

    const grid = document.getElementById('workspace-grid');
    grid.innerHTML = workspaces.map((w) => `
      <div class="card" data-id="${w.id}">
        <p class="card-title">${escapeHtml(w.name)}</p>
        <p class="card-meta">${escapeHtml(w.slug)} · ${roleBadge(w.myRole)}</p>
      </div>`).join('') + `
      <div class="card card-add" id="add-workspace-card">+ New workspace</div>`;

    grid.querySelectorAll('.card[data-id]').forEach((card) => {
      card.addEventListener('click', () => { location.hash = `#/w/${card.dataset.id}`; });
    });
    document.getElementById('add-workspace-card').addEventListener('click', openCreateWorkspaceModal);
  }

  function openCreateWorkspaceModal() {
    const body = openModal('New workspace', `
      <form id="create-workspace-form">
        <div class="field">
          <label for="ws-name">Name</label>
          <input id="ws-name" type="text" required maxlength="120">
        </div>
        <p class="error-text" id="form-error"></p>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" id="cancel-btn">Cancel</button>
          <button type="submit" class="btn">Create</button>
        </div>
      </form>`);
    body.querySelector('#cancel-btn').addEventListener('click', closeModal);
    body.querySelector('#create-workspace-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('ws-name').value.trim();
      const errorEl = document.getElementById('form-error');
      try {
        const ws = await api('POST', '/api/workspaces', { name });
        closeModal();
        toast('Workspace created', 'success');
        location.hash = `#/w/${ws.id}`;
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  // ---------- workspace detail ----------

  async function renderWorkspaceDetail(workspaceId, tab) {
    const activeTab = tab === 'members' ? 'members' : 'projects';
    const workspace = await api('GET', `/api/workspaces/${workspaceId}`);

    setBreadcrumbs([
      { label: 'Workspaces', hash: '/workspaces' },
      { label: workspace.name },
    ]);

    appRoot.innerHTML = `
      <div class="page-header">
        <div>
          <h1>${escapeHtml(workspace.name)} ${roleBadge(workspace.myRole)}</h1>
          <div class="subtitle">${escapeHtml(workspace.slug)}</div>
        </div>
      </div>
      <div class="tabs">
        <button class="tab ${activeTab === 'projects' ? 'active' : ''}" id="tab-projects">Projects</button>
        <button class="tab ${activeTab === 'members' ? 'active' : ''}" id="tab-members">Members</button>
      </div>
      <div id="tab-content"></div>`;

    document.getElementById('tab-projects').addEventListener('click', () => renderWorkspaceTab(workspace, 'projects'));
    document.getElementById('tab-members').addEventListener('click', () => renderWorkspaceTab(workspace, 'members'));

    await renderWorkspaceTab(workspace, activeTab);
  }

  async function renderWorkspaceTab(workspace, tab) {
    document.getElementById('tab-projects').classList.toggle('active', tab === 'projects');
    document.getElementById('tab-members').classList.toggle('active', tab === 'members');
    const content = document.getElementById('tab-content');
    content.innerHTML = '<div class="loading">Loading…</div>';

    try {
      if (tab === 'projects') {
        const projects = await api('GET', `/api/workspaces/${workspace.id}/projects`);
        content.innerHTML = `<div class="card-grid" id="project-grid"></div>`;
        const grid = document.getElementById('project-grid');
        grid.innerHTML = projects.map((p) => `
          <div class="card" data-id="${p.id}">
            <p class="card-title">${escapeHtml(p.key)} · ${escapeHtml(p.name)}</p>
            <p class="card-meta">${p.myRole ? roleBadge(p.myRole) : 'Not a member'}</p>
          </div>`).join('') + `<div class="card card-add" id="add-project-card">+ New project</div>`;
        grid.querySelectorAll('.card[data-id]').forEach((card) => {
          card.addEventListener('click', () => { location.hash = `#/w/${workspace.id}/p/${card.dataset.id}`; });
        });
        document.getElementById('add-project-card').addEventListener('click', () => openCreateProjectModal(workspace));
      } else {
        const members = await api('GET', `/api/workspaces/${workspace.id}/members`);
        const canManage = canManageWorkspace(workspace);
        content.innerHTML = `
          ${canManage ? '<div class="row" style="justify-content:flex-end;margin-bottom:10px;"><button class="btn btn-sm" id="add-member-btn">+ Add member</button></div>' : ''}
          <table class="table">
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Joined</th></tr></thead>
            <tbody>
              ${members.map((m) => `
                <tr>
                  <td>${escapeHtml(m.displayName)}</td>
                  <td>${escapeHtml(m.email)}</td>
                  <td>${roleBadge(m.role)}</td>
                  <td>${statusBadge(m.status)}</td>
                  <td>${formatDate(m.createdAt)}</td>
                </tr>`).join('')}
            </tbody>
          </table>`;
        if (canManage) {
          document.getElementById('add-member-btn').addEventListener('click', () => openAddWorkspaceMemberModal(workspace));
        }
      }
    } catch (err) {
      content.innerHTML = `<div class="empty-state">${escapeHtml(err.message)}</div>`;
    }
  }

  function openCreateProjectModal(workspace) {
    const body = openModal('New project', `
      <form id="create-project-form">
        <div class="field">
          <label for="p-key">Key</label>
          <input id="p-key" type="text" required maxlength="10" placeholder="e.g. ENG" style="text-transform:uppercase">
          <span class="field-hint">Letters and numbers, starts with a letter. Used as the issue prefix (e.g. ENG-1).</span>
        </div>
        <div class="field">
          <label for="p-name">Name</label>
          <input id="p-name" type="text" required maxlength="120">
        </div>
        <div class="field">
          <label for="p-desc">Description</label>
          <textarea id="p-desc"></textarea>
        </div>
        <p class="error-text" id="form-error"></p>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" id="cancel-btn">Cancel</button>
          <button type="submit" class="btn">Create</button>
        </div>
      </form>`);
    body.querySelector('#cancel-btn').addEventListener('click', closeModal);
    body.querySelector('#create-project-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const key = document.getElementById('p-key').value.trim();
      const name = document.getElementById('p-name').value.trim();
      const description = document.getElementById('p-desc').value.trim();
      const errorEl = document.getElementById('form-error');
      try {
        const project = await api('POST', `/api/workspaces/${workspace.id}/projects`, { key, name, description: description || null });
        closeModal();
        toast('Project created', 'success');
        location.hash = `#/w/${workspace.id}/p/${project.id}`;
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  function openAddWorkspaceMemberModal(workspace) {
    const body = openModal('Add workspace member', `
      <form id="add-member-form">
        <div class="field">
          <label for="m-email">Email</label>
          <input id="m-email" type="email" required>
          <span class="field-hint">If this email has no easyTask account yet, one will be created as a pending invite.</span>
        </div>
        <div class="field">
          <label for="m-display-name">Display name</label>
          <input id="m-display-name" type="text" maxlength="120">
          <span class="field-hint">Only used when inviting a brand-new user. Ignored for existing accounts.</span>
        </div>
        <div class="field">
          <label for="m-role">Role</label>
          <select id="m-role">
            <option value="MEMBER">Member</option>
            <option value="ADMIN">Admin</option>
            <option value="OWNER">Owner</option>
          </select>
        </div>
        <p class="error-text" id="form-error"></p>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" id="cancel-btn">Cancel</button>
          <button type="submit" class="btn">Add</button>
        </div>
      </form>`);
    body.querySelector('#cancel-btn').addEventListener('click', closeModal);
    body.querySelector('#add-member-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('m-email').value.trim();
      const displayName = document.getElementById('m-display-name').value.trim();
      const role = document.getElementById('m-role').value;
      const errorEl = document.getElementById('form-error');
      try {
        const member = await api('POST', `/api/workspaces/${workspace.id}/members`, { email, role, displayName: displayName || null });
        closeModal();
        if (member.inviteToken) {
          showInviteLinkModal(member.email, member.inviteToken);
        } else {
          toast('Member added', 'success');
        }
        renderWorkspaceTab(workspace, 'members');
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  function showInviteLinkModal(email, inviteToken) {
    const link = `${location.origin}/#/accept-invite/${inviteToken}`;
    const body = openModal('Invite created', `
      <p>${escapeHtml(email)} doesn't have an account yet. Share this link with them to set a password and join:</p>
      <div class="field">
        <input id="invite-link" type="text" readonly value="${escapeHtml(link)}">
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-ghost" id="copy-link-btn">Copy link</button>
        <button type="button" class="btn" id="close-invite-btn">Done</button>
      </div>`);
    body.querySelector('#close-invite-btn').addEventListener('click', closeModal);
    body.querySelector('#copy-link-btn').addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(link);
        toast('Link copied', 'success');
      } catch {
        body.querySelector('#invite-link').select();
      }
    });
  }

  // ---------- project detail ----------

  async function renderProjectDetail(workspaceId, projectId, tab) {
    const activeTab = tab === 'members' ? 'members' : 'board';
    const [workspace, project] = await Promise.all([
      api('GET', `/api/workspaces/${workspaceId}`),
      api('GET', `/api/workspaces/${workspaceId}/projects/${projectId}`),
    ]);

    setBreadcrumbs([
      { label: 'Workspaces', hash: '/workspaces' },
      { label: workspace.name, hash: `/w/${workspace.id}` },
      { label: project.key },
    ]);

    appRoot.innerHTML = `
      <div class="project-shell">
        <aside class="app-sidebar" aria-label="Project navigation">
          <nav>
            <button class="nav-item" id="nav-board" type="button">Board</button>
            <span class="nav-item nav-item-disabled" title="Coming soon">Issues</span>
            <span class="nav-item nav-item-disabled" title="Coming soon">Reports</span>
            <span class="nav-item nav-item-disabled" title="Coming soon">Process rules</span>
          </nav>
        </aside>
        <div class="project-main">
          <div class="project-main-header">
            <div>
              <h1>${escapeHtml(project.key)} · ${escapeHtml(project.name)} ${project.myRole ? roleBadge(project.myRole) : ''}</h1>
              <div class="subtitle">${escapeHtml(project.description || '')}</div>
            </div>
            <div id="board-actions" class="row"></div>
          </div>
          <div id="tab-content"></div>
        </div>
      </div>`;

    document.getElementById('nav-board').addEventListener('click', () => renderProjectTab(workspace, project, 'board'));

    await renderProjectTab(workspace, project, activeTab);
  }

  async function renderProjectTab(workspace, project, tab) {
    document.getElementById('nav-board').classList.toggle('active', tab === 'board');
    const content = document.getElementById('tab-content');
    const actions = document.getElementById('board-actions');
    content.innerHTML = '<div class="loading">Loading…</div>';
    actions.innerHTML = '<button class="btn btn-ghost btn-sm" id="members-link-btn" type="button">Members</button>';
    actions.querySelector('#members-link-btn').addEventListener('click', () => renderProjectTab(workspace, project, 'members'));

    try {
      if (tab === 'board') {
        const [statuses, issueTypes, issues, members] = await Promise.all([
          api('GET', `/api/workspaces/${workspace.id}/projects/${project.id}/statuses`),
          api('GET', `/api/workspaces/${workspace.id}/projects/${project.id}/issue-types`),
          api('GET', `/api/workspaces/${workspace.id}/projects/${project.id}/issues`),
          api('GET', `/api/workspaces/${workspace.id}/projects/${project.id}/members`),
        ]);
        statuses.sort((a, b) => a.position - b.position);
        members.forEach((m) => { projectMembersCache[m.userId] = m.displayName; });
        const canWrite = canWriteProject(workspace, project);

        if (canWrite) {
          actions.insertAdjacentHTML('beforeend', '<button class="btn btn-sm" id="new-issue-btn">+ New issue</button>');
          actions.querySelector('#new-issue-btn').addEventListener('click', () =>
            openIssuePanel(workspace, project, { statuses, issueTypes }, null));
        }

        content.innerHTML = `<div class="board" id="board"></div>`;
        const board = document.getElementById('board');

        if (statuses.length === 0) {
          board.outerHTML = '<div class="empty-state">No statuses configured for this project yet.</div>';
          return;
        }

        board.innerHTML = statuses.map((status) => {
          const columnIssues = issues
            .filter((i) => i.statusId === status.id)
            .sort((a, b) => Number(a.position) - Number(b.position));
          return `
            <div class="board-column" data-status-id="${status.id}">
              <div class="board-column-header">
                <span>${escapeHtml(status.name)}</span>
                <span class="board-column-count">${columnIssues.length}</span>
              </div>
              ${columnIssues.map((issue) => `
                <div class="issue-card" data-issue-id="${issue.id}" ${canWrite ? 'draggable="true"' : ''}>
                  <div class="issue-card-key">${escapeHtml(issue.key)} · ${escapeHtml(issue.issueTypeName)}</div>
                  <div class="issue-card-title">${escapeHtml(issue.title)}</div>
                  <div class="issue-card-footer">
                    ${priorityBadge(issue.priority)}
                    ${issue.assigneeId ? `<span class="avatar" title="${escapeHtml(memberLabel(workspace, issue.assigneeId))}">${escapeHtml(initials(memberLabel(workspace, issue.assigneeId)))}</span>` : ''}
                  </div>
                </div>`).join('')}
            </div>`;
        }).join('');

        board.querySelectorAll('.issue-card').forEach((card) => {
          card.addEventListener('click', async () => {
            const issue = issues.find((i) => i.id === card.dataset.issueId);
            openIssuePanel(workspace, project, { statuses, issueTypes }, issue);
          });
        });

        if (canWrite) {
          board.querySelectorAll('.issue-card[draggable="true"]').forEach((card) => {
            card.addEventListener('dragstart', (e) => {
              e.dataTransfer.setData('text/plain', card.dataset.issueId);
              card.classList.add('dragging');
            });
            card.addEventListener('dragend', () => card.classList.remove('dragging'));
          });

          board.querySelectorAll('.board-column').forEach((column) => {
            column.addEventListener('dragover', (e) => {
              e.preventDefault();
              column.classList.add('drag-over');
            });
            column.addEventListener('dragleave', () => column.classList.remove('drag-over'));
            column.addEventListener('drop', async (e) => {
              e.preventDefault();
              column.classList.remove('drag-over');
              const draggedId = e.dataTransfer.getData('text/plain');
              if (!draggedId) return;
              const { prevIssueId, nextIssueId } = dropTarget(column, draggedId, e.clientY);
              try {
                await api('PATCH', `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${draggedId}/move`, {
                  statusId: column.dataset.statusId,
                  prevIssueId,
                  nextIssueId,
                });
                renderProjectTab(workspace, project, 'board');
              } catch (err) {
                toast(err.message);
              }
            });
          });
        }
      } else {
        const members = await api('GET', `/api/workspaces/${workspace.id}/projects/${project.id}/members`);
        if (canManageProject(workspace, project)) {
          actions.insertAdjacentHTML('beforeend', '<button class="btn btn-sm" id="add-project-member-btn">+ Add member</button>');
          actions.querySelector('#add-project-member-btn').addEventListener('click', () =>
            openAddProjectMemberModal(workspace, project));
        }
        content.innerHTML = `
          <table class="table">
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Joined</th></tr></thead>
            <tbody>
              ${members.map((m) => `
                <tr>
                  <td>${escapeHtml(m.displayName)}</td>
                  <td>${escapeHtml(m.email)}</td>
                  <td>${roleBadge(m.role)}</td>
                  <td>${statusBadge(m.status)}</td>
                  <td>${formatDate(m.createdAt)}</td>
                </tr>`).join('')}
            </tbody>
          </table>`;
      }
    } catch (err) {
      content.innerHTML = `<div class="empty-state">${escapeHtml(err.message)}</div>`;
    }
  }

  function dropTarget(column, draggedId, clientY) {
    const cards = Array.from(column.querySelectorAll('.issue-card')).filter((el) => el.dataset.issueId !== draggedId);
    let index = cards.length;
    for (let i = 0; i < cards.length; i++) {
      const rect = cards[i].getBoundingClientRect();
      if (clientY < rect.top + rect.height / 2) {
        index = i;
        break;
      }
    }
    return {
      prevIssueId: index > 0 ? cards[index - 1].dataset.issueId : null,
      nextIssueId: index < cards.length ? cards[index].dataset.issueId : null,
    };
  }

  // simple in-memory cache so issue cards/modals can show an assignee's email without another round trip
  const projectMembersCache = {};

  function memberLabel(workspace, userId) {
    const cached = projectMembersCache[userId];
    return cached || userId.slice(0, 8);
  }

  async function openAddProjectMemberModal(workspace, project) {
    const body = openModal('Add project member', `
      <form id="add-pm-form">
        <div class="field">
          <label for="pm-email">Email</label>
          <input id="pm-email" type="email" required>
          <span class="field-hint">The user must already be a member of this workspace.</span>
        </div>
        <div class="field">
          <label for="pm-role">Role</label>
          <select id="pm-role">
            <option value="MEMBER">Member</option>
            <option value="LEAD">Lead</option>
            <option value="VIEWER">Viewer</option>
          </select>
        </div>
        <p class="error-text" id="form-error"></p>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" id="cancel-btn">Cancel</button>
          <button type="submit" class="btn">Add</button>
        </div>
      </form>`);
    body.querySelector('#cancel-btn').addEventListener('click', closeModal);
    body.querySelector('#add-pm-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('pm-email').value.trim();
      const role = document.getElementById('pm-role').value;
      const errorEl = document.getElementById('form-error');
      try {
        await api('POST', `/api/workspaces/${workspace.id}/projects/${project.id}/members`, { email, role });
        closeModal();
        toast('Member added', 'success');
        renderProjectTab(workspace, project, 'members');
      } catch (err) {
        errorEl.textContent = err.message;
      }
    });
  }

  // ---------- issue panel (view / edit / create) ----------

  async function openIssuePanel(workspace, project, { issueTypes }, issue) {
    const isCreate = !issue;
    const canWrite = canWriteProject(workspace, project);
    const members = await api('GET', `/api/workspaces/${workspace.id}/projects/${project.id}/members`);
    members.forEach((m) => { projectMembersCache[m.userId] = m.displayName; });

    const body = openPanel(isCreate ? 'New issue' : issue.key, `
      <div id="issue-area"></div>
      ${!isCreate ? `
        <div class="comments-section">
          <h4>Comments</h4>
          <div id="comments-list" class="comments-list"></div>
          ${canWrite ? `
            <form id="comment-form" class="comment-form">
              <textarea id="comment-body" placeholder="Add a comment…" required></textarea>
              <div class="modal-footer">
                <button type="submit" class="btn btn-sm">Comment</button>
              </div>
            </form>` : ''}
        </div>
        <div class="history-section">
          <h4>History</h4>
          <div id="history-list" class="history-list"></div>
        </div>` : ''}`);
    const area = body.querySelector('#issue-area');

    const statusOptions = isCreate ? [] : await api('GET',
      `/api/workspaces/${workspace.id}/projects/${project.id}/issue-types/${issue.projectIssueTypeId}/status-options`);

    function closeAndRefresh() {
      closePanel();
      renderProjectTab(workspace, project, 'board');
    }

    function assigneeOptionsHtml(selectedId) {
      return ['<option value="">Unassigned</option>']
        .concat(members.map((m) => `<option value="${m.userId}" ${selectedId === m.userId ? 'selected' : ''}>${escapeHtml(m.displayName)}</option>`))
        .join('');
    }

    function priorityOptionsHtml(selected) {
      return ['LOW', 'MEDIUM', 'HIGH'].map((p) =>
        `<option value="${p}" ${p === selected ? 'selected' : ''}>${p}</option>`).join('');
    }

    function renderView() {
      area.innerHTML = `
        <div class="issue-view-top">
          ${priorityBadge(issue.priority)}
          <span class="issue-view-type">${escapeHtml(issue.issueTypeName)}</span>
        </div>
        <h2 class="issue-view-title">${escapeHtml(issue.title)}</h2>
        <p class="issue-view-desc">${issue.description ? escapeHtml(issue.description) : '<span class="issue-view-empty">No description.</span>'}</p>
        <div class="issue-view-meta">
          <div class="issue-view-meta-row"><span class="issue-view-meta-label">Assignee</span><span>${issue.assigneeId ? escapeHtml(memberLabel(workspace, issue.assigneeId)) : 'Unassigned'}</span></div>
          <div class="issue-view-meta-row"><span class="issue-view-meta-label">Due date</span><span>${issue.dueDate ? formatDate(issue.dueDate) : '—'}</span></div>
        </div>
        <div class="field">
          <label for="issue-status">Status</label>
          <select id="issue-status" ${canWrite ? '' : 'disabled'}></select>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost" id="close-btn">Close</button>
          ${canWrite ? '<button type="button" class="btn" id="edit-btn">Edit</button>' : ''}
        </div>`;

      const statusSelect = area.querySelector('#issue-status');
      statusSelect.innerHTML = statusOptions.map((s) =>
        `<option value="${s.id}" ${s.id === issue.statusId ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('');

      area.querySelector('#close-btn').addEventListener('click', closeAndRefresh);
      if (canWrite) {
        area.querySelector('#edit-btn').addEventListener('click', renderEdit);
        statusSelect.addEventListener('change', async () => {
          try {
            await api('PATCH', `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}/status`,
              { statusId: statusSelect.value });
            issue.statusId = statusSelect.value;
            toast('Status updated', 'success');
            loadHistory(workspace, project, issue, document.getElementById('history-list'));
          } catch (err) {
            toast(err.message);
          }
        });
      }
    }

    function renderEdit() {
      area.innerHTML = `
        <form id="issue-form">
          <div class="field">
            <label for="issue-title">Title</label>
            <input id="issue-title" type="text" required maxlength="255" value="${escapeHtml(issue.title)}">
          </div>
          <div class="field">
            <label for="issue-desc">Description</label>
            <textarea id="issue-desc">${escapeHtml(issue.description || '')}</textarea>
          </div>
          <div class="field">
            <label for="issue-priority">Priority</label>
            <select id="issue-priority">${priorityOptionsHtml(issue.priority)}</select>
          </div>
          <div class="field">
            <label for="issue-assignee">Assignee</label>
            <select id="issue-assignee">${assigneeOptionsHtml(issue.assigneeId)}</select>
          </div>
          <div class="field">
            <label for="issue-due">Due date</label>
            <input id="issue-due" type="date" value="${issue.dueDate || ''}">
          </div>
          <p class="error-text" id="form-error"></p>
          <div class="modal-footer">
            <button type="button" class="btn btn-ghost" id="cancel-edit-btn">Cancel</button>
            <button type="submit" class="btn">Save</button>
          </div>
        </form>`;

      area.querySelector('#cancel-edit-btn').addEventListener('click', renderView);
      area.querySelector('#issue-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = area.querySelector('#form-error');
        const titleVal = area.querySelector('#issue-title').value.trim();
        const descVal = area.querySelector('#issue-desc').value.trim();
        const priorityVal = area.querySelector('#issue-priority').value;
        const assigneeVal = area.querySelector('#issue-assignee').value || null;
        const dueVal = area.querySelector('#issue-due').value || null;
        try {
          await api('PATCH', `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}`, {
            title: titleVal,
            description: descVal || null,
            priority: priorityVal,
            assigneeId: assigneeVal,
            dueDate: dueVal,
          });
          issue.title = titleVal;
          issue.description = descVal;
          issue.priority = priorityVal;
          issue.assigneeId = assigneeVal;
          issue.dueDate = dueVal;
          toast('Issue updated', 'success');
          renderView();
        } catch (err) {
          errorEl.textContent = err.message;
        }
      });
    }

    function renderCreate() {
      area.innerHTML = `
        <form id="issue-form">
          <div class="field">
            <label for="issue-type">Type</label>
            <select id="issue-type" required>
              ${issueTypes.map((t) => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('')}
            </select>
          </div>
          <div class="field">
            <label for="issue-title">Title</label>
            <input id="issue-title" type="text" required maxlength="255">
          </div>
          <div class="field">
            <label for="issue-desc">Description</label>
            <textarea id="issue-desc"></textarea>
          </div>
          <div class="field">
            <label for="issue-priority">Priority</label>
            <select id="issue-priority">${priorityOptionsHtml('MEDIUM')}</select>
          </div>
          <div class="field">
            <label for="issue-assignee">Assignee</label>
            <select id="issue-assignee">${assigneeOptionsHtml(null)}</select>
          </div>
          <div class="field">
            <label for="issue-due">Due date</label>
            <input id="issue-due" type="date">
          </div>
          <p class="error-text" id="form-error"></p>
          <div class="modal-footer">
            <button type="button" class="btn btn-ghost" id="close-btn">Close</button>
            <button type="submit" class="btn">Create</button>
          </div>
        </form>`;

      area.querySelector('#close-btn').addEventListener('click', closeAndRefresh);
      area.querySelector('#issue-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const errorEl = area.querySelector('#form-error');
        const typeVal = area.querySelector('#issue-type').value;
        const titleVal = area.querySelector('#issue-title').value.trim();
        const descVal = area.querySelector('#issue-desc').value.trim();
        const priorityVal = area.querySelector('#issue-priority').value;
        const assigneeVal = area.querySelector('#issue-assignee').value || null;
        const dueVal = area.querySelector('#issue-due').value || null;
        try {
          await api('POST', `/api/workspaces/${workspace.id}/projects/${project.id}/issues`, {
            projectIssueTypeId: typeVal,
            title: titleVal,
            description: descVal || null,
            priority: priorityVal,
            assigneeId: assigneeVal,
            dueDate: dueVal,
          });
          toast('Issue created', 'success');
          closeAndRefresh();
        } catch (err) {
          errorEl.textContent = err.message;
        }
      });
    }

    if (isCreate) {
      renderCreate();
      return;
    }

    renderView();
    loadHistory(workspace, project, issue, document.getElementById('history-list'));
    loadComments(workspace, project, issue, document.getElementById('comments-list'));
    const commentForm = document.getElementById('comment-form');
    if (commentForm) {
      commentForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const textarea = document.getElementById('comment-body');
        const bodyVal = textarea.value.trim();
        if (!bodyVal) return;
        try {
          await api('POST', `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}/comments`, { body: bodyVal });
          textarea.value = '';
          loadComments(workspace, project, issue, document.getElementById('comments-list'));
        } catch (err) {
          toast(err.message);
        }
      });
    }
  }

  // ---------- issue comments ----------

  async function loadComments(workspace, project, issue, listEl) {
    listEl.innerHTML = '<div class="loading">Loading…</div>';
    try {
      const comments = await api('GET',
        `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}/comments`);
      const currentUserId = getUserId();

      listEl.innerHTML = comments.length === 0
        ? '<div class="empty-state">No comments yet.</div>'
        : comments.map((c) => `
          <div class="comment-item" data-comment-id="${c.id}">
            <div class="comment-header">
              <span class="comment-author">${escapeHtml(c.authorName)}</span>
              <span class="comment-date">${formatDate(c.createdAt)}${c.editedAt ? ' (edited)' : ''}</span>
            </div>
            <div class="comment-body">${escapeHtml(c.body)}</div>
            ${c.authorId === currentUserId ? `
              <div class="comment-actions">
                <button type="button" class="link-btn" data-action="edit">Edit</button>
                <button type="button" class="link-btn" data-action="delete">Delete</button>
              </div>` : ''}
          </div>`).join('');

      listEl.querySelectorAll('.comment-item').forEach((item) => {
        const comment = comments.find((c) => c.id === item.dataset.commentId);
        const editBtn = item.querySelector('[data-action="edit"]');
        const deleteBtn = item.querySelector('[data-action="delete"]');
        if (editBtn) {
          editBtn.addEventListener('click', () => startEditComment(workspace, project, issue, comment, item, listEl));
        }
        if (deleteBtn) {
          deleteBtn.addEventListener('click', async () => {
            if (!confirm('Delete this comment?')) return;
            try {
              await api('DELETE',
                `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}/comments/${comment.id}`);
              loadComments(workspace, project, issue, listEl);
            } catch (err) {
              toast(err.message);
            }
          });
        }
      });
    } catch (err) {
      listEl.innerHTML = `<div class="empty-state">${escapeHtml(err.message)}</div>`;
    }
  }

  function startEditComment(workspace, project, issue, comment, itemEl, listEl) {
    itemEl.innerHTML = `
      <form class="comment-edit-form">
        <textarea required>${escapeHtml(comment.body)}</textarea>
        <div class="modal-footer">
          <button type="button" class="btn btn-ghost btn-sm" data-action="cancel">Cancel</button>
          <button type="submit" class="btn btn-sm">Save</button>
        </div>
      </form>`;
    itemEl.querySelector('[data-action="cancel"]').addEventListener('click', () =>
      loadComments(workspace, project, issue, listEl));
    itemEl.querySelector('.comment-edit-form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const bodyVal = itemEl.querySelector('textarea').value.trim();
      try {
        await api('PATCH',
          `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}/comments/${comment.id}`,
          { body: bodyVal });
        loadComments(workspace, project, issue, listEl);
      } catch (err) {
        toast(err.message);
      }
    });
  }

  // ---------- issue history ----------

  const HISTORY_FIELD_LABELS = {
    title: 'Title',
    description: 'Description',
    priority: 'Priority',
    assignee: 'Assignee',
    dueDate: 'Due date',
    status: 'Status',
  };

  function historyValueText(field, value) {
    if (value === null || value === undefined || value === '') return '—';
    if (field === 'description' && value.length > 60) return `${value.slice(0, 60)}…`;
    return value;
  }

  async function loadHistory(workspace, project, issue, listEl) {
    listEl.innerHTML = '<div class="loading">Loading…</div>';
    try {
      const entries = await api('GET',
        `/api/workspaces/${workspace.id}/projects/${project.id}/issues/${issue.id}/history`);

      listEl.innerHTML = entries.length === 0
        ? '<div class="empty-state">No history yet.</div>'
        : entries.map((h) => `
          <div class="history-item">
            <span class="history-author">${escapeHtml(h.actorName)}</span>
            changed <span class="history-field">${escapeHtml(HISTORY_FIELD_LABELS[h.field] || h.field)}</span>:
            <span class="history-old">${escapeHtml(historyValueText(h.field, h.oldValue))}</span>
            →
            <span class="history-new">${escapeHtml(historyValueText(h.field, h.newValue))}</span>
            <span class="history-date">${formatDate(h.createdAt)}</span>
          </div>`).join('');
    } catch (err) {
      listEl.innerHTML = `<div class="empty-state">${escapeHtml(err.message)}</div>`;
    }
  }
})();

(function() {
    'use strict';

    var settingsBtn = document.getElementById('settings-btn');
    var modal = null;
    var currentConfig = null;
    var seedContent = null;

    settingsBtn.addEventListener('click', function() {
        openSettings();
    });

    function openSettings() {
        if (modal) {
            modal.style.display = 'flex';
            return;
        }
        createModal();
        modal.style.display = 'flex';
        loadConfig();
        loadSeedContent();
    }

    function closeModal() {
        if (modal) modal.style.display = 'none';
    }

    function createModal() {
        modal = document.createElement('div');
        modal.id = 'settings-modal';
        modal.innerHTML = '\
            <div class="settings-panel">\
                <div class="settings-header">\
                    <h2>设置</h2>\
                    <button id="settings-close" class="settings-close-btn">×</button>\
                </div>\
                <div class="settings-body">\
                    <div class="settings-tabs">\
                        <button class="settings-tab active" data-tab="global">全局</button>\
                        <button class="settings-tab" data-tab="models">模型</button>\
                        <button class="settings-tab" data-tab="mcps">MCP</button>\
                        <button class="settings-tab" data-tab="reference">参考备注</button>\
                    </div>\
                    <div class="settings-tab-content" id="tab-global">\
                        <div class="form-row">\
                            <label>默认模型</label>\
                            <select id="cfg-default-model"><option value="">-- 未设置 --</option></select>\
                        </div>\
                        <div class="form-row">\
                            <label>最大迭代次数</label>\
                            <input type="number" id="cfg-max-iterations" min="1" placeholder="50">\
                        </div>\
                        <div class="form-row">\
                            <label>命令执行超时(秒)</label>\
                            <input type="number" id="cfg-exec-timeout" min="1" placeholder="30">\
                        </div>\
                    </div>\
                    <div class="settings-tab-content" id="tab-models" style="display:none">\
                        <div id="models-list"></div>\
                        <button type="button" id="add-model-btn" class="add-btn">+ 添加模型</button>\
                    </div>\
                    <div class="settings-tab-content" id="tab-mcps" style="display:none">\
                        <div id="mcps-list"></div>\
                        <button type="button" id="add-mcp-btn" class="add-btn">+ 添加 MCP</button>\
                    </div>\
                    <div class="settings-tab-content" id="tab-reference" style="display:none">\
                        <pre id="seed-content" class="seed-pre"></pre>\
                    </div>\
                </div>\
                <div class="settings-footer">\
                    <button type="button" id="settings-save" class="save-btn">保存</button>\
                </div>\
            </div>';
        document.body.appendChild(modal);

        document.getElementById('settings-close').addEventListener('click', closeModal);
        modal.addEventListener('click', function(e) {
            if (e.target === modal) closeModal();
        });

        document.querySelectorAll('.settings-tab').forEach(function(btn) {
            btn.addEventListener('click', function() {
                document.querySelectorAll('.settings-tab').forEach(function(b) { b.classList.remove('active'); });
                document.querySelectorAll('.settings-tab-content').forEach(function(c) { c.style.display = 'none'; });
                btn.classList.add('active');
                document.getElementById('tab-' + btn.dataset.tab).style.display = 'block';
            });
        });

        document.getElementById('add-model-btn').addEventListener('click', addModelRow);
        document.getElementById('add-mcp-btn').addEventListener('click', addMcpRow);
        document.getElementById('settings-save').addEventListener('click', saveConfig);

        addStyle();
    }

    function addStyle() {
        var style = document.createElement('style');
        style.textContent = '\
            #settings-modal { position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);z-index:1000;display:flex;align-items:center;justify-content:center; }\
            .settings-panel { background:#fff;border-radius:10px;width:680px;max-height:85vh;display:flex;flex-direction:column;box-shadow:0 8px 32px rgba(0,0,0,0.15); }\
            .settings-header { display:flex;align-items:center;justify-content:space-between;padding:16px 20px;border-bottom:1px solid #e5e2ec; }\
            .settings-header h2 { margin:0;font-size:18px;color:#333; }\
            .settings-close-btn { background:none;border:none;font-size:24px;cursor:pointer;color:#888;padding:0;line-height:1; }\
            .settings-close-btn:hover { color:#333; }\
            .settings-body { flex:1;overflow-y:auto;padding:16px 20px; }\
            .settings-tabs { display:flex;gap:8px;margin-bottom:16px; }\
            .settings-tab { padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;cursor:pointer;font-size:14px;color:#555; }\
            .settings-tab.active { background:#4a306d;color:#fff;border-color:#4a306d; }\
            .settings-tab-content { }\
            .form-row { display:flex;align-items:center;margin-bottom:12px;gap:12px; }\
            .form-row label { width:140px;flex-shrink:0;font-size:14px;color:#555; }\
            .form-row input, .form-row select { flex:1;padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:14px; }\
            .models-list-item, .mcps-list-item { border:1px solid #e5e2ec;border-radius:8px;padding:12px;margin-bottom:10px; }\
            .models-list-item .row-grid, .mcps-list-item .row-grid { display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px; }\
            .models-list-item .row-grid-3 { display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;margin-bottom:8px; }\
            .models-list-item input, .models-list-item select, .mcps-list-item input { padding:5px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:13px; }\
            .models-list-item .field-label, .mcps-list-item .field-label { font-size:12px;color:#888;margin-bottom:2px; }\
            .models-list-item .del-btn, .mcps-list-item .del-btn { background:none;border:none;color:#e53e3e;cursor:pointer;font-size:13px;padding:2px 6px; }\
            .models-list-item .del-btn:hover, .mcps-list-item .del-btn:hover { color:#c53030; }\
            .add-btn { background:#f0ebfa;border:1px solid #8b5cf6;color:#6b4db8;border-radius:6px;padding:6px 16px;cursor:pointer;font-size:13px; }\
            .add-btn:hover { background:#e5dcf7; }\
            .settings-footer { padding:12px 20px;border-top:1px solid #e5e2ec;display:flex;justify-content:flex-end; }\
            .save-btn { background:#4a306d;color:#fff;border:none;border-radius:6px;padding:8px 24px;font-size:14px;cursor:pointer; }\
            .save-btn:hover { background:#3a2060; }\
            .seed-pre { background:#f7f7f7;border:1px solid #e5e2ec;border-radius:6px;padding:12px;font-size:12px;overflow:auto;max-height:400px;white-space:pre-wrap;word-break:break-all; }\
            .error-msg { color:#e53e3e;font-size:13px;margin-top:4px; }\
            .toast { position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:#333;color:#fff;padding:8px 20px;border-radius:6px;font-size:14px;z-index:2000; }';
        document.head.appendChild(style);
    }

    function loadConfig() {
        fetch('/api/config').then(function(r) { return r.json(); }).then(function(data) {
            currentConfig = data;
            populateForm(data);
        }).catch(function(e) {
            showToast('加载配置失败: ' + e.message);
        });
    }

    function loadSeedContent() {
        fetch('/api/config/default').then(function(r) { return r.json(); }).then(function(data) {
            seedContent = data.content || '';
            document.getElementById('seed-content').textContent = seedContent;
        }).catch(function() {});
    }

    function populateForm(data) {
        var cfg = data.config || data;

        var defaultModelSelect = document.getElementById('cfg-default-model');
        defaultModelSelect.innerHTML = '<option value="">-- 未设置 --</option>';
        var models = cfg.models || {};
        Object.keys(models).forEach(function(key) {
            var opt = document.createElement('option');
            opt.value = key;
            opt.textContent = key + (models[key].description ? ' - ' + models[key].description : '');
            defaultModelSelect.appendChild(opt);
        });
        if (cfg.defaultModel) defaultModelSelect.value = cfg.defaultModel;

        document.getElementById('cfg-max-iterations').value = cfg.maxIterations || '';
        document.getElementById('cfg-exec-timeout').value = cfg.execTimeoutSeconds || '';

        renderModels(models);
        renderMcps(cfg.mcpServers || {});
    }

    function renderModels(models) {
        var container = document.getElementById('models-list');
        container.innerHTML = '';
        var keys = Object.keys(models);
        if (keys.length === 0) {
            addModelRow();
            return;
        }
        keys.forEach(function(key) {
            addModelRow(key, models[key]);
        });
    }

    function addModelRow(key, model) {
        key = key || '';
        model = model || {};
        var container = document.getElementById('models-list');
        var div = document.createElement('div');
        div.className = 'models-list-item';
        div.innerHTML = '\
            <div class="row-grid-3">\
                <div>\
                    <div class="field-label">Key *</div>\
                    <input type="text" name="model-key" value="' + escHtml(key) + '" placeholder="e.g. minimax">\
                </div>\
                <div>\
                    <div class="field-label">模型名 *</div>\
                    <input type="text" name="model-name" value="' + escHtml(model.model || '') + '" placeholder="e.g. MiniMax-M2.7">\
                </div>\
                <div>\
                    <div class="field-label">API Base *</div>\
                    <input type="text" name="model-base" value="' + escHtml(model.apiBase || '') + '" placeholder="https://api.example.com/v1">\
                </div>\
            </div>\
            <div class="row-grid">\
                <div>\
                    <div class="field-label">API Key</div>\
                    <input type="text" name="model-key2" value="' + escHtml(model.apiKey || '') + '" placeholder="sk-...">\
                </div>\
                <div>\
                    <div class="field-label">描述</div>\
                    <input type="text" name="model-desc" value="' + escHtml(model.description || '') + '" placeholder="模型描述">\
                </div>\
            </div>\
            <div class="row-grid">\
                <div>\
                    <div class="field-label">最大上下文(k)</div>\
                    <input type="text" name="model-ctx" value="' + escHtml(model.maxContextSize || '') + '" placeholder="200k">\
                </div>\
                <div>\
                    <div class="field-label">最大 Tokens</div>\
                    <input type="number" name="model-maxtokens" value="' + (model.maxTokens || '') + '" placeholder="2048">\
                </div>\
                <div>\
                    <div class="field-label">Temperature</div>\
                    <input type="number" name="model-temp" step="0.1" value="' + (model.temperature != null ? model.temperature : '') + '" placeholder="0.7">\
                </div>\
            </div>\
            <button type="button" class="del-btn">删除</button>';
        div.querySelector('.del-btn').addEventListener('click', function() {
            div.remove();
        });
        container.appendChild(div);
    }

    function renderMcps(mcps) {
        var container = document.getElementById('mcps-list');
        container.innerHTML = '';
        var keys = Object.keys(mcps);
        if (keys.length === 0) {
            addMcpRow();
            return;
        }
        keys.forEach(function(key) {
            addMcpRow(key, mcps[key]);
        });
    }

    function addMcpRow(key, mcp) {
        key = key || '';
        mcp = mcp || {};
        var container = document.getElementById('mcps-list');
        var div = document.createElement('div');
        div.className = 'mcps-list-item';
        div.innerHTML = '\
            <div class="row-grid">\
                <div>\
                    <div class="field-label">Key *</div>\
                    <input type="text" name="mcp-key" value="' + escHtml(key) + '" placeholder="e.g. MiniMax">\
                </div>\
                <div>\
                    <div class="field-label">描述</div>\
                    <input type="text" name="mcp-desc" value="' + escHtml(mcp.description || '') + '" placeholder="MCP 描述">\
                </div>\
            </div>\
            <div class="row-grid">\
                <div>\
                    <div class="field-label">Command *</div>\
                    <input type="text" name="mcp-cmd" value="' + escHtml(mcp.command || '') + '" placeholder="uvx">\
                </div>\
                <div>\
                    <div class="field-label">Args(逗号分隔)</div>\
                    <input type="text" name="mcp-args" value="' + escHtml((mcp.args || []).join(', ')) + '" placeholder="arg1, arg2">\
                </div>\
            </div>\
            <div>\
                <div class="field-label">Env(KEY=value 每行一个)</div>\
                <textarea name="mcp-env" rows="3" style="width:100%;border:1px solid #d1d5db;border-radius:4px;padding:5px 8px;font-size:13px;">' + escHtml(envToText(mcp.env || {})) + '</textarea>\
            </div>\
            <button type="button" class="del-btn">删除</button>';
        div.querySelector('.del-btn').addEventListener('click', function() {
            div.remove();
        });
        container.appendChild(div);
    }

    function envToText(env) {
        return Object.keys(env).map(function(k) { return k + '=' + env[k]; }).join('\n');
    }

    function parseEnv(text) {
        var env = {};
        text.split('\n').forEach(function(line) {
            line = line.trim();
            if (!line) return;
            var idx = line.indexOf('=');
            if (idx > 0) {
                env[line.substring(0, idx).trim()] = line.substring(idx + 1).trim();
            }
        });
        return env;
    }

    function saveConfig() {
        var cfg = {
            defaultModel: document.getElementById('cfg-default-model').value || null,
            maxIterations: parseInt(document.getElementById('cfg-max-iterations').value) || null,
            execTimeoutSeconds: parseInt(document.getElementById('cfg-exec-timeout').value) || null,
            models: {},
            mcpServers: {}
        };

        var errors = [];

        document.querySelectorAll('.models-list-item').forEach(function(div) {
            var key = div.querySelector('[name="model-key"]').value.trim();
            var model = div.querySelector('[name="model-name"]').value.trim();
            var apiBase = div.querySelector('[name="model-base"]').value.trim();
            if (!key) {
                errors.push('模型 Key 不能为空');
                return;
            }
            if (!model) errors.push('models.' + key + '.model 不能为空');
            if (!apiBase) errors.push('models.' + key + '.apiBase 不能为空');
            cfg.models[key] = {
                model: model,
                apiBase: apiBase,
                apiKey: div.querySelector('[name="model-key2"]').value,
                description: div.querySelector('[name="model-desc"]').value,
                maxContextSize: div.querySelector('[name="model-ctx"]').value || null,
                maxTokens: parseInt(div.querySelector('[name="model-maxtokens"]').value) || null,
                temperature: parseFloat(div.querySelector('[name="model-temp"]').value) || null
            };
        });

        document.querySelectorAll('.mcps-list-item').forEach(function(div) {
            var key = div.querySelector('[name="mcp-key"]').value.trim();
            var command = div.querySelector('[name="mcp-cmd"]').value.trim();
            if (!key) {
                errors.push('MCP Key 不能为空');
                return;
            }
            if (!command) errors.push('mcpServers.' + key + '.command 不能为空');
            var argsText = div.querySelector('[name="mcp-args"]').value;
            var args = argsText ? argsText.split(',').map(function(s) { return s.trim(); }).filter(Boolean) : [];
            cfg.mcpServers[key] = {
                command: command,
                description: div.querySelector('[name="mcp-desc"]').value,
                args: args,
                env: parseEnv(div.querySelector('[name="mcp-env"]').value)
            };
        });

        if (cfg.defaultModel && !cfg.models[cfg.defaultModel]) {
            errors.push('defaultModel 指向不存在的模型: ' + cfg.defaultModel);
        }

        if (errors.length > 0) {
            showToast(errors[0]);
            return;
        }

        fetch('/api/config', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(cfg)
        }).then(function(r) {
            if (r.ok) {
                r.json().then(function(data) {
                    showToast('配置已保存，新配置将在新会话生效');
                    closeModal();
                });
            } else {
                r.json().then(function(data) {
                    var msg = data.error || '保存失败';
                    if (data.details) msg += ': ' + data.details.join(', ');
                    showToast(msg);
                }).catch(function() {
                    showToast('保存失败: HTTP ' + r.status);
                });
            }
        }).catch(function(e) {
            showToast('保存失败: ' + e.message);
        });
    }

    function escHtml(s) {
        if (!s) return '';
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function showToast(msg) {
        var t = document.createElement('div');
        t.className = 'toast';
        t.textContent = msg;
        document.body.appendChild(t);
        setTimeout(function() { t.remove(); }, 3500);
    }

})();

INSERT INTO public.permissions (name, http_method, api_path, description)
VALUES ('admin.dashboard.stats', 'GET', '/api/admin/dashboard/stats', 'Admin dashboard stats')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN' AND p.name = 'admin.dashboard.stats'
ON CONFLICT DO NOTHING;

INSERT INTO public.permissions (name, http_method, api_path, description)
VALUES ('ai.agent.performance', 'GET', '/api/admin/ai/agent/*/performance',
        'AI agent performance analysis')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN' AND p.name = 'ai.agent.performance'
ON CONFLICT DO NOTHING;
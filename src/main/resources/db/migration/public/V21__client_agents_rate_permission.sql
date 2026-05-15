INSERT INTO public.permissions (name, http_method, api_path, description)
VALUES ('agent.rate', 'PATCH', '/api/users/agents/*/rate', 'Rate an agent')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name = 'CLIENT' AND p.name = 'agent.rate'
ON CONFLICT DO NOTHING;

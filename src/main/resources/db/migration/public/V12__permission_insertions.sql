INSERT INTO public.roles (name, description) VALUES
                                                 ('ADMIN',  'Full system access'),
                                                 ('AGENT',  'Real estate agent — manages properties and contracts'),
                                                 ('CLIENT', 'End client — browse and apply for properties');


INSERT INTO public.permissions (name, http_method, api_path, description) VALUES

                                                                              -- PROPERTIES
                                                                              ('property.read.all',       'GET',    '/api/properties',                    'List all properties'),
                                                                              ('property.read.one',       'GET',    '/api/properties/*',                  'Get single property'),
                                                                              ('property.search',         'GET',    '/api/properties/search',             'Search properties'),
                                                                              ('property.filter',         'GET',    '/api/properties/filter',             'Filter properties'),
                                                                              ('property.featured',       'GET',    '/api/properties/featured',           'Get featured properties'),
                                                                              ('property.by.agent',       'GET',    '/api/properties/agent/*',            'Properties by agent'),
                                                                              ('property.create',         'POST',   '/api/properties',                    'Create property'),
                                                                              ('property.update',         'PUT',    '/api/properties/*',                  'Update property'),
                                                                              ('property.delete',         'DELETE', '/api/properties/*',                  'Delete property'),
                                                                              ('property.status',         'PATCH',  '/api/properties/*/status',           'Change status'),
                                                                              ('property.image.upload',   'POST',   '/api/properties/*/images',           'Upload image'),
                                                                              ('property.image.delete',   'DELETE', '/api/properties/*/images/*',         'Delete image'),
                                                                              ('property.image.primary',  'PATCH',  '/api/properties/*/images/*/primary', 'Set primary image'),
                                                                              ('property.price.history',  'GET',    '/api/properties/*/price-history',    'Price history'),
                                                                              ('property.save',           'POST',   '/api/properties/saved',              'Save property'),
                                                                              ('property.unsave',         'DELETE', '/api/properties/saved/*',            'Unsave property'),
                                                                              ('property.saved.list',     'GET',    '/api/properties/saved',              'Saved properties'),

                                                                              -- RENTALS
                                                                              ('rental.listing.read',     'GET',    '/api/rentals/listings',              'List rentals'),
                                                                              ('rental.listing.read.one', 'GET',    '/api/rentals/listings/*',            'Get rental'),
                                                                              ('rental.listing.create',   'POST',   '/api/rentals/listings',              'Create rental listing'),
                                                                              ('rental.listing.update',   'PUT',    '/api/rentals/listings/*',            'Update rental listing'),
                                                                              ('rental.listing.delete',   'DELETE', '/api/rentals/listings/*',            'Delete rental listing'),
                                                                              ('rental.apply',            'POST',   '/api/rentals/applications',          'Apply for rental'),
                                                                              ('rental.application.read', 'GET',    '/api/rentals/applications',          'List applications'),
                                                                              ('rental.application.review','PATCH', '/api/rentals/applications/*/review', 'Review application'),
                                                                              ('rental.application.cancel','PATCH', '/api/rentals/applications/*/cancel', 'Cancel application'),

                                                                              -- LEASE CONTRACTS
                                                                              ('contract.read.all',       'GET',    '/api/contracts/lease',               'List all contracts'),
                                                                              ('contract.read.own',       'GET',    '/api/contracts/lease/client/*',      'Own contracts'),
                                                                              ('contract.read.one',       'GET',    '/api/contracts/lease/*',             'Get contract'),
                                                                              ('contract.create',         'POST',   '/api/contracts/lease',               'Create contract'),
                                                                              ('contract.status',         'PATCH',  '/api/contracts/lease/*/status',      'Change status'),

                                                                              -- PAYMENTS
                                                                              ('payment.read.all',        'GET',    '/api/payments',                      'List all payments'),
                                                                              ('payment.read.own',        'GET',    '/api/payments/client/*',             'Own payments'),
                                                                              ('payment.read.one',        'GET',    '/api/payments/*',                    'Get payment'),
                                                                              ('payment.mark.paid',       'PATCH',  '/api/payments/*/paid',               'Mark as paid'),

                                                                              -- SALES
                                                                              ('sale.listing.read',       'GET',    '/api/sales/listings',                'List sales'),
                                                                              ('sale.listing.read.one',   'GET',    '/api/sales/listings/*',              'Get sale listing'),
                                                                              ('sale.listing.create',     'POST',   '/api/sales/listings',                'Create sale listing'),
                                                                              ('sale.listing.update',     'PUT',    '/api/sales/listings/*',              'Update sale listing'),
                                                                              ('sale.listing.delete',     'DELETE', '/api/sales/listings/*',              'Delete sale listing'),
                                                                              ('sale.apply',              'POST',   '/api/sales/applications',            'Apply to buy'),
                                                                              ('sale.application.read',   'GET',    '/api/sales/applications',            'List applications'),
                                                                              ('sale.application.my',     'GET',    '/api/sales/applications/my',         'My applications'),
                                                                              ('sale.application.cancel', 'PATCH',  '/api/sales/applications/*/cancel',   'Cancel application'),
                                                                              ('sale.contract.create',    'POST',   '/api/sales/contracts',               'Create sale contract'),
                                                                              ('sale.contract.read',      'GET',    '/api/sales/contracts',               'List sale contracts'),
                                                                              ('sale.contract.status',    'PATCH',  '/api/sales/contracts/*/status',      'Change contract status'),
                                                                              ('sale.payment.read',       'GET',    '/api/sales/payments',                'List sale payments'),
                                                                              ('sale.payment.paid',       'PATCH',  '/api/sales/payments/*/paid',         'Mark sale payment paid'),

                                                                              -- LEADS
                                                                              ('lead.create',             'POST',   '/api/leads',                         'Create lead'),
                                                                              ('lead.read.all',           'GET',    '/api/leads',                         'List all leads'),
                                                                              ('lead.read.own',           'GET',    '/api/leads/my/client',               'Own leads'),
                                                                              ('lead.read.one',           'GET',    '/api/leads/*',                       'Get lead'),
                                                                              ('lead.assign',             'PATCH',  '/api/leads/*/assign',                'Assign lead'),
                                                                              ('lead.status',             'PATCH',  '/api/leads/*/status',                'Update lead status'),
                                                                              ('lead.decline',            'PATCH',  '/api/leads/*/decline',               'Decline lead'),

                                                                              -- MAINTENANCE
                                                                              ('maintenance.create',      'POST',   '/api/maintenance',                   'Create request'),
                                                                              ('maintenance.read.all',    'GET',    '/api/maintenance',                   'List all'),
                                                                              ('maintenance.read.own',    'GET',    '/api/maintenance/my',                'Own requests'),
                                                                              ('maintenance.read.one',    'GET',    '/api/maintenance/*',                 'Get request'),
                                                                              ('maintenance.update',      'PUT',    '/api/maintenance/*',                 'Update request'),
                                                                              ('maintenance.assign',      'PATCH',  '/api/maintenance/*/assign',          'Assign request'),
                                                                              ('maintenance.status',      'PATCH',  '/api/maintenance/*/status',          'Update status'),

                                                                              -- NOTIFICATIONS
                                                                              ('notification.read',       'GET',    '/api/notifications',                 'List notifications'),
                                                                              ('notification.unread',     'GET',    '/api/notifications/unread',          'Unread notifications'),
                                                                              ('notification.count',      'GET',    '/api/notifications/unread/count',    'Unread count'),
                                                                              ('notification.read.one',   'PATCH',  '/api/notifications/*/read',          'Mark one read'),
                                                                              ('notification.read.all',   'PATCH',  '/api/notifications/read-all',        'Mark all read'),
                                                                              ('notification.delete',     'DELETE', '/api/notifications/read',            'Delete read'),

                                                                              -- USERS
                                                                              ('user.read.all',           'GET',    '/api/users',                         'List all users'),
                                                                              ('user.read.one',           'GET',    '/api/users/*',                       'Get user'),
                                                                              ('user.read.me',            'GET',    '/api/users/me',                      'Own profile'),
                                                                              ('user.update.me',          'PUT',    '/api/users/me',                      'Update own profile'),
                                                                              ('user.password',           'PATCH',  '/api/users/me/password',             'Change password'),
                                                                              ('user.manage',             'PATCH',  '/api/users/*',                       'Manage users'),
                                                                              ('user.delete',             'DELETE', '/api/users/*',                       'Delete user'),
                                                                              ('user.agents.list',        'GET',    '/api/users/agents/list',             'List agents'),
                                                                              ('user.agents.read',        'GET',    '/api/users/agents/*',                'Get agent'),
                                                                              ('user.agents.update.me',   'PUT',    '/api/users/agents/me',               'Update agent profile'),
                                                                              ('user.agents.update',      'PUT',    '/api/users/agents/*',                'Update any agent'),
                                                                              ('user.clients.me',         'GET',    '/api/users/clients/me',              'Own client profile'),
                                                                              ('user.clients.update.me',  'PUT',    '/api/users/clients/me',              'Update client profile'),
                                                                              ('user.clients.read',       'GET',    '/api/users/clients/*',               'Get client'),

                                                                              -- AI
                                                                              ('ai.description',          'POST',   '/api/ai/property/description',       'AI description'),
                                                                              ('ai.estimate',             'POST',   '/api/ai/property/estimate',          'AI price estimate'),
                                                                              ('ai.chat',                 'POST',   '/api/ai/chat',                       'AI chat'),
                                                                              ('ai.contract.summary',     'POST',   '/api/ai/contract/summary',           'AI contract summary'),
                                                                              ('ai.risk',                 'GET',    '/api/ai/payments/risk/*',            'AI risk analysis'),
                                                                              ('ai.leads.match',          'POST',   '/api/ai/leads/match',                'AI lead match'),

                                                                              -- ADMIN
                                                                              ('admin.tenants',           'GET',    '/api/admin/tenants',                 'List tenants'),
                                                                              ('admin.tenants.create',    'POST',   '/api/admin/tenants',                 'Create tenant'),
                                                                              ('admin.tenants.manage',    'PATCH',  '/api/admin/tenants/*',               'Manage tenant'),
                                                                              ('admin.permissions.list',  'GET',    '/api/admin/permissions',             'List permissions'),
                                                                              ('admin.permissions.grant', 'POST',   '/api/admin/permissions/roles/*',     'Grant permission'),
                                                                              ('admin.permissions.revoke','DELETE', '/api/admin/permissions/roles/*',     'Revoke permission'),
                                                                              ('admin.roles.list',        'GET',    '/api/admin/roles',                   'List roles');


INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN';


INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
         JOIN public.permissions p ON p.name IN (
                                                 'property.read.all','property.read.one','property.search','property.filter',
                                                 'property.featured','property.by.agent','property.create','property.update',
                                                 'property.delete','property.status','property.image.upload','property.image.delete',
                                                 'property.image.primary','property.price.history','property.save',
                                                 'property.unsave','property.saved.list',
                                                 'rental.listing.read','rental.listing.read.one','rental.listing.create',
                                                 'rental.listing.update','rental.listing.delete','rental.apply',
                                                 'rental.application.read','rental.application.review','rental.application.cancel',
                                                 'contract.read.all','contract.read.own','contract.read.one','contract.create','contract.status',
                                                 'payment.read.all','payment.read.own','payment.read.one','payment.mark.paid',
                                                 'sale.listing.read','sale.listing.read.one','sale.listing.create','sale.listing.update',
                                                 'sale.listing.delete','sale.apply','sale.application.read','sale.application.my',
                                                 'sale.application.cancel','sale.contract.create','sale.contract.read',
                                                 'sale.contract.status','sale.payment.read','sale.payment.paid',
                                                 'lead.create','lead.read.all','lead.read.own','lead.read.one','lead.status','lead.decline',
                                                 'maintenance.create','maintenance.read.all','maintenance.read.own','maintenance.read.one',
                                                 'maintenance.update','maintenance.assign','maintenance.status',
                                                 'notification.read','notification.unread','notification.count',
                                                 'notification.read.one','notification.read.all','notification.delete',
                                                 'user.read.me','user.update.me','user.password',
                                                 'user.agents.list','user.agents.read','user.agents.update.me',
                                                 'user.clients.me','user.clients.update.me',
                                                 'ai.description','ai.estimate','ai.chat','ai.contract.summary','ai.risk','ai.leads.match'
    )
WHERE r.name = 'AGENT';


INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
         JOIN public.permissions p ON p.name IN (
                                                 'property.read.all','property.read.one','property.search','property.filter',
                                                 'property.featured','property.save','property.unsave','property.saved.list',
                                                 'rental.listing.read','rental.listing.read.one','rental.apply','rental.application.cancel',
                                                 'contract.read.own','contract.read.one',
                                                 'payment.read.own','payment.read.one',
                                                 'sale.listing.read','sale.listing.read.one','sale.apply',
                                                 'sale.application.my','sale.application.cancel',
                                                 'lead.create','lead.read.own',
                                                 'maintenance.create','maintenance.read.own','maintenance.read.one',
                                                 'notification.read','notification.unread','notification.count',
                                                 'notification.read.one','notification.read.all','notification.delete',
                                                 'user.read.me','user.update.me','user.password',
                                                 'user.clients.me','user.clients.update.me',
                                                 'ai.chat'
    )
WHERE r.name = 'CLIENT';


INSERT INTO public.user_roles (user_id, role_id)
SELECT u.id, r.id
FROM public.users u
         JOIN public.roles r ON r.name = u.role
WHERE u.deleted_at IS NULL;
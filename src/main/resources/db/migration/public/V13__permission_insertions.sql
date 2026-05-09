
INSERT INTO public.permissions (name, http_method, api_path, description) VALUES

                                                                              -- PROPERTIES
                                                                              ('property.image.list',        'GET',    '/api/properties/*/images',               'List property images'),
                                                                              ('property.save.id',           'POST',   '/api/properties/saved/*',                'Save property by ID'),

                                                                              -- RENTALS
                                                                              ('rental.listing.by.property', 'GET',    '/api/rentals/listings/property/*',       'Rental listings by property'),
                                                                              ('rental.app.by.listing',      'GET',    '/api/rentals/applications/listing/*',    'Rental applications by listing'),
                                                                              ('rental.application.my',      'GET',    '/api/rentals/applications/my',           'My rental applications'),

                                                                              -- LEASE CONTRACTS
                                                                              ('contract.read.agent',        'GET',    '/api/contracts/lease/agent/*',           'Contracts by agent'),
                                                                              ('contract.read.property',     'GET',    '/api/contracts/lease/property/*',        'Contracts by property'),
                                                                              ('contract.expiring',          'GET',    '/api/contracts/lease/expiring',          'Expiring contracts'),
                                                                              ('contract.update',            'PUT',    '/api/contracts/lease/*',                 'Update lease contract'),

                                                                              -- PAYMENTS
                                                                              ('payment.contract',           'GET',    '/api/payments/contract/*',               'Payments by contract'),
                                                                              ('payment.summary',            'GET',    '/api/payments/contract/*/summary',       'Payment summary'),
                                                                              ('payment.by.status',          'GET',    '/api/payments/status/*',                 'Payments by status'),
                                                                              ('payment.overdue',            'GET',    '/api/payments/overdue',                  'Overdue payments'),
                                                                              ('payment.revenue',            'GET',    '/api/payments/revenue',                  'Total revenue'),
                                                                              ('payment.create',             'POST',   '/api/payments',                          'Create payment'),
                                                                              ('payment.pay',                'PATCH',  '/api/payments/*/pay',                    'Mark payment as paid'),
                                                                              ('payment.status.update',      'PATCH',  '/api/payments/*/status',                 'Update payment status'),

                                                                              -- NOTIFICATIONS
                                                                              ('notification.create',        'POST',   '/api/notifications',                     'Create notification (ADMIN)'),

                                                                              -- LEADS
                                                                              ('lead.read.agent',            'GET',    '/api/leads/my/agent',                    'My assigned leads'),
                                                                              ('lead.unassigned',            'GET',    '/api/leads/unassigned',                  'Unassigned leads'),
                                                                              ('lead.by.property',           'GET',    '/api/leads/property/*',                  'Leads by property'),
                                                                              ('lead.link.property',         'PATCH',  '/api/leads/*/property',                  'Link property to lead'),

                                                                              -- MAINTENANCE
                                                                              ('maintenance.by.property',    'GET',    '/api/maintenance/property/*',            'Maintenance by property'),
                                                                              ('maintenance.assigned',       'GET',    '/api/maintenance/assigned',              'Assigned to me'),
                                                                              ('maintenance.urgent',         'GET',    '/api/maintenance/urgent',                'Urgent requests'),

                                                                              -- SALES LISTINGS
                                                                              ('sale.listing.agent.me',      'GET',    '/api/sales/listings/agent/me',           'My sale listings'),
                                                                              ('sale.listing.by.status',     'GET',    '/api/sales/listings/status/*',           'Listings by status'),
                                                                              ('sale.listing.by.property',   'GET',    '/api/sales/listings/property/*',         'Listings by property'),

                                                                              -- SALE APPLICATIONS
                                                                              ('sale.app.read.one',          'GET',    '/api/sales/applications/*',              'Get sale application'),
                                                                              ('sale.app.by.listing',        'GET',    '/api/sales/applications/listing/*',      'Applications by listing'),
                                                                              ('sale.app.by.property',       'GET',    '/api/sales/applications/property/*',     'Applications by property'),
                                                                              ('sale.app.agent.me',          'GET',    '/api/sales/applications/agent/me',       'My agent applications'),
                                                                              ('sale.app.by.status',         'GET',    '/api/sales/applications/status/*',       'Applications by status'),
                                                                              ('sale.app.status',            'PATCH',  '/api/sales/applications/*/status',       'Update application status'),

                                                                              -- SALE CONTRACTS
                                                                              ('sale.contract.read.one',     'GET',    '/api/sales/contracts/*',                 'Get sale contract'),
                                                                              ('sale.contract.buyer',        'GET',    '/api/sales/contracts/buyer/*',           'Contracts by buyer'),
                                                                              ('sale.contract.agent',        'GET',    '/api/sales/contracts/agent/*',           'Contracts by agent'),
                                                                              ('sale.contract.property',     'GET',    '/api/sales/contracts/property/*',        'Contracts by property'),
                                                                              ('sale.contract.update',       'PUT',    '/api/sales/contracts/*',                 'Update sale contract'),

                                                                              -- SALE PAYMENTS
                                                                              ('sale.payment.by.contract',   'GET',    '/api/sales/payments/contract/*',         'Sale payments by contract'),
                                                                              ('sale.payment.summary',       'GET',    '/api/sales/payments/contract/*/summary', 'Sale payment summary'),
                                                                              ('sale.payment.create',        'POST',   '/api/sales/payments',                    'Create sale payment'),

                                                                              -- USERS
                                                                              ('user.agents.all',            'GET',    '/api/users/agents',                      'List all agents with profiles'),
                                                                              ('user.agents.me',             'GET',    '/api/users/agents/me',                   'My agent profile'),
                                                                              ('user.status',                'PATCH',  '/api/users/*/status',                    'Set user active status'),
                                                                              ('user.role',                  'PATCH',  '/api/users/*/role',                      'Change user role'),

                                                                              -- ADMIN TENANTS
                                                                              ('admin.tenant.read.one',      'GET',    '/api/admin/tenants/*',                   'Get tenant by ID'),
                                                                              ('admin.tenant.deactivate',    'PATCH',  '/api/admin/tenants/*/deactivate',        'Deactivate tenant')

ON CONFLICT (name) DO NOTHING;



INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;



INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
         JOIN public.permissions p ON p.name IN (
                                                 'property.image.list',
                                                 'property.save.id',
                                                 'rental.listing.by.property',
                                                 'rental.app.by.listing',
                                                 'rental.application.my',
                                                 'contract.read.agent',
                                                 'contract.read.property',
                                                 'contract.expiring',
                                                 'contract.update',
                                                 'payment.contract',
                                                 'payment.summary',
                                                 'payment.by.status',
                                                 'payment.overdue',
                                                 'payment.revenue',
                                                 'payment.create',
                                                 'payment.pay',
                                                 'payment.status.update',
                                                 'lead.read.agent',
                                                 'lead.unassigned',
                                                 'lead.by.property',
                                                 'lead.link.property',
                                                 'maintenance.by.property',
                                                 'maintenance.assigned',
                                                 'maintenance.urgent',
                                                 'sale.listing.agent.me',
                                                 'sale.listing.by.status',
                                                 'sale.listing.by.property',
                                                 'sale.app.read.one',
                                                 'sale.app.by.listing',
                                                 'sale.app.by.property',
                                                 'sale.app.agent.me',
                                                 'sale.app.by.status',
                                                 'sale.app.status',
                                                 'sale.contract.read.one',
                                                 'sale.contract.buyer',
                                                 'sale.contract.agent',
                                                 'sale.contract.property',
                                                 'sale.contract.update',
                                                 'sale.payment.by.contract',
                                                 'sale.payment.summary',
                                                 'sale.payment.create',
                                                 'user.agents.all',
                                                 'user.agents.me'
    )
WHERE r.name = 'AGENT'
ON CONFLICT DO NOTHING;



INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
         JOIN public.permissions p ON p.name IN (
                                                 'property.image.list',
                                                 'property.save.id',
                                                 'rental.listing.by.property',
                                                 'rental.application.my',
                                                 'payment.contract',
                                                 'payment.summary',
                                                 'payment.by.status',
                                                 'sale.listing.by.status',
                                                 'sale.listing.by.property',
                                                 'sale.app.read.one',
                                                 'sale.contract.read.one',
                                                 'sale.contract.buyer',
                                                 'sale.payment.by.contract',
                                                 'sale.payment.summary',
                                                 'user.agents.all',
                                                 'user.agents.me'
    )
WHERE r.name = 'CLIENT'
ON CONFLICT DO NOTHING;
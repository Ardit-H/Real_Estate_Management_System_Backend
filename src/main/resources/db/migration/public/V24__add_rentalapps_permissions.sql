
INSERT INTO public.permissions (name, http_method, api_path, description) VALUES
                                                                              ('rental.listings.read',           'GET',   '/api/rentals/listings',                    'Get all rental listings'),
                                                                              ('rental.listings.read.one',       'GET',   '/api/rentals/listings/*',                  'Get rental listing by id'),
                                                                              ('rental.listings.read.property',  'GET',   '/api/rentals/listings/property/*',         'Get listings by property'),
                                                                              ('rental.listings.create',         'POST',  '/api/rentals/listings',                    'Create rental listing'),
                                                                              ('rental.listings.update',         'PUT',   '/api/rentals/listings/*',                  'Update rental listing'),
                                                                              ('rental.listings.delete',         'DELETE','/api/rentals/listings/*',                  'Delete rental listing'),
                                                                              ('rental.applications.create',     'POST',  '/api/rentals/applications',                'Apply for rental listing'),
                                                                              ('rental.applications.by.listing', 'GET',   '/api/rentals/applications/listing/*',      'Get applications by listing'),
                                                                              ('rental.applications.my',         'GET',   '/api/rentals/applications/my',             'Get my applications'),
                                                                              ('rental.applications.review',     'PATCH', '/api/rentals/applications/*/review',       'Review rental application'),
                                                                              ('rental.applications.cancel',     'PATCH', '/api/rentals/applications/*/cancel',       'Cancel rental application')
ON CONFLICT (name) DO NOTHING;

-- ADMIN — të gjitha
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
                 'rental.listings.read', 'rental.listings.read.one', 'rental.listings.read.property',
                 'rental.listings.create', 'rental.listings.update', 'rental.listings.delete',
                 'rental.applications.create', 'rental.applications.by.listing',
                 'rental.applications.my', 'rental.applications.review', 'rental.applications.cancel'
    )
ON CONFLICT DO NOTHING;

-- AGENT — listings + shqyrtim aplikimesh
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r, public.permissions p
WHERE r.name = 'AGENT'
  AND p.name IN (
                 'rental.listings.read', 'rental.listings.read.one', 'rental.listings.read.property',
                 'rental.listings.create', 'rental.listings.update', 'rental.listings.delete',
                 'rental.applications.by.listing', 'rental.applications.review'
    )
ON CONFLICT DO NOTHING;

-- CLIENT — apliko + anulo + shiko të miat
INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r, public.permissions p
WHERE r.name = 'CLIENT'
  AND p.name IN (
                 'rental.listings.read', 'rental.listings.read.one', 'rental.listings.read.property',
                 'rental.applications.create', 'rental.applications.my', 'rental.applications.cancel'
    )
ON CONFLICT DO NOTHING;
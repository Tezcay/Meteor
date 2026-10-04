UPDATE users
SET password_hash = '!demo-account-disabled!',
    status = 'LOCKED'
WHERE email IN ('buyer@meteor.local', 'creator@meteor.local');

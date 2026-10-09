import unittest

import migrate_urls as m

SOURCE = 'https://old.example.test'
TARGET = 'https://new.example.test'


class MigrationTests(unittest.TestCase):
    def setUp(self):
        self.db = m.connect()
        if self.db.db not in ('migration_test', b'migration_test'):
            raise RuntimeError('Tests require disposable migration_test database')
        with self.db.cursor() as c:
            for table, pk in m.TABLES.items():
                c.execute(f'DROP TABLE IF EXISTS `{table}`')
                c.execute(f'CREATE TABLE `{table}` (`{pk}` INT PRIMARY KEY, image_url VARCHAR(512), '
                          'is_current BOOL DEFAULT 1, deleted_at DATETIME NULL) ENGINE=InnoDB')
        self.add('user_photo', 1, m.public_url(SOURCE, 'user-photo/한글 +%.png'))
        self.add('club_photo', 1, SOURCE + '/' + m.DEFAULT_KEY)
        self.add('promo_photo', 1, 'https://kakao.example.test/profile.png')
        self.keys = {m.DEFAULT_KEY, 'user-photo/한글 +%.png'}

    def tearDown(self):
        self.db.close()

    def add(self, table, pk, url, current=1):
        with self.db.cursor() as c:
            c.execute(f'INSERT INTO `{table}` (`{m.TABLES[table]}`,image_url,is_current) VALUES (%s,%s,%s)',
                      (pk, url, current))
        self.db.commit()

    def value(self, table, pk=1):
        with self.db.cursor() as c:
            c.execute(f'SELECT image_url FROM `{table}` WHERE `{m.TABLES[table]}`=%s', (pk,))
            return c.fetchone()['image_url']

    def test_round_trip_idempotence_and_url_boundaries(self):
        for value in [SOURCE + '.evil.test/a', SOURCE + '/%2e%2e/a', SOURCE + '/a?x=1',
                      SOURCE + '/%FF', SOURCE + '/x%ZZ', SOURCE + '/a#x', SOURCE + ':444/a']:
            self.assertIsNone(m.managed_key(value, SOURCE))
        manifest = m.plan(self.db, SOURCE, TARGET, self.keys)
        self.assertEqual(2, len(manifest['rows']))
        self.assertEqual(2, m.apply(self.db, manifest, 2)['changed'])
        self.assertEqual(2, m.apply(self.db, manifest, 2)['already_applied'])
        self.assertEqual('user-photo/한글 +%.png', m.managed_key(self.value('user_photo'), TARGET))
        self.assertEqual('https://kakao.example.test/profile.png', self.value('promo_photo'))
        self.assertEqual(2, m.apply(self.db, manifest, 2, rollback=True)['changed'])
        self.assertEqual(SOURCE + '/' + m.DEFAULT_KEY, self.value('club_photo'))

    def test_stale_row_rolls_back_whole_transaction_and_case_sensitive_match(self):
        manifest = m.plan(self.db, SOURCE, TARGET, self.keys)
        with self.db.cursor() as c:
            c.execute('UPDATE club_photo SET image_url=%s WHERE club_photo_id=1',
                      ((SOURCE + '/' + m.DEFAULT_KEY).upper(),))
        self.db.commit()
        with self.assertRaisesRegex(ValueError, 'Row changed'):
            m.apply(self.db, manifest, 2)
        self.assertEqual(m.public_url(SOURCE, 'user-photo/한글 +%.png'), self.value('user_photo'))
        with self.assertRaises(ValueError):
            m.apply(self.db, manifest, 1)

    def test_missing_objects_prevent_active_row_migration(self):
        self.add('notice', 1, SOURCE + '/notice-photo/missing.png')
        self.add('user_photo', 2, SOURCE + '/user-photo/deleted.png', current=0)
        manifest = m.plan(self.db, SOURCE, TARGET, self.keys)
        self.assertFalse(manifest['applicable'])
        self.assertEqual(1, manifest['summary']['notice']['missing_active'])
        self.assertEqual(1, manifest['summary']['user_photo']['missing_inactive'])
        with self.assertRaises(ValueError):
            m.apply(self.db, manifest, 2)
        self.assertEqual(m.public_url(SOURCE, 'user-photo/한글 +%.png'), self.value('user_photo'))


if __name__ == '__main__':
    unittest.main()

-- Cache co version tren Redis. KEYS[1] = key; ARGV = version, du lieu, 'V' (gia tri) | 'T' (tombstone), ttl ms.
-- Quy tac: tombstone thang neu version >= hien tai; gia tri thang neu version > hien tai,
-- hoac bang version cua mot tombstone (DB dang dung o version do, gia tri nay la moi nhat).
local cur = redis.call('HMGET', KEYS[1], 'v', 'k')
local curV = tonumber(cur[1])
local newV = tonumber(ARGV[1])
local kind = ARGV[3]
if curV then
  if kind == 'T' and newV < curV then return 0 end
  if kind == 'V' and (newV < curV or (newV == curV and cur[2] == 'V')) then return 0 end
end
redis.call('HSET', KEYS[1], 'v', newV, 'd', ARGV[2], 'k', kind)
redis.call('PEXPIRE', KEYS[1], ARGV[4])
return 1

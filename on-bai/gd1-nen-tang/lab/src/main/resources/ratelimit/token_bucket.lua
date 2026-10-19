-- Token bucket nguyen tu tren Redis. KEYS[1] = bucket; ARGV = capacity, refill/giay, so token can.
-- Ca script chay MOT MACH tren Redis (don luong) -> khong instance nao chen vao giua doc va ghi.
local t = redis.call('TIME')                                 -- dong ho cua Redis: moi instance app dung chung
local now = tonumber(t[1]) * 1000000 + tonumber(t[2])        -- micro giay
local capacity = tonumber(ARGV[1])
local per_us = tonumber(ARGV[2]) / 1000000
local want = tonumber(ARGV[3])

local state = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(state[1]) or capacity
local ts = tonumber(state[2]) or now
tokens = math.min(capacity, tokens + math.max(0, now - ts) * per_us)

local allowed = 0
if tokens >= want then
  tokens = tokens - want
  allowed = 1
end
redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
-- bucket day lai sau capacity/refill giay: het han luc do, key nguoi khong song mai
redis.call('PEXPIRE', KEYS[1], math.ceil(capacity / per_us / 1000) + 1000)
return {allowed, math.floor(tokens)}

-- Align with polymorphic SpecificData jsonb (spot/perp/future/option).
-- Historical column held option-only payloads; rename for all market types.
do $$
begin
  if exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'markets'
      and column_name = 'option_specific_data'
  ) and not exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'markets'
      and column_name = 'specific_data'
  ) then
    alter table public.markets rename column option_specific_data to specific_data;
  elsif not exists (
    select 1
    from information_schema.columns
    where table_schema = 'public'
      and table_name = 'markets'
      and column_name = 'specific_data'
  ) then
    alter table public.markets add column specific_data jsonb;
  end if;
end $$;

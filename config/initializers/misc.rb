Time::DATE_FORMATS[:short_us] = "%b %d, %Y %H:%M"

class Array
  # Quick convenient way to convert array of objects to hash keyed on value determined by passed-in block.
  #   x = %w(a bb ccc)
  #   x.to_hash(&:size)    #  {1=>"a", 2=>"bb", 3=>"ddd"}
  #   [1, 2, 3].to_hash    #  {1=>1, 2=>2, 3=>3}
  def to_map(&block)
    inject({}) do |h, e|
      block_given? ? key = yield(e) : key = e
      h[key] = e
      h
    end
  end

  # Quick convenient way to convert array of objects to hash of key/value pair determined by passed-in block.
  #   x = %w(a bb ccc)
  #   x.to_hash_with_value {|e| return e, e.size}    #  {"a" +> 1, "bb" => 2, "ddd" => 3}
  def to_map_with_value(&block)
    inject({}) do |h, e|
      key, value = yield(e)
      h[key] = value
      h
    end
  end

  def info
    @info ||= {}
  end
end

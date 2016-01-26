class Cms::CiMd < ActiveResource::Base
  self.prefix = "/adapter/rest/md/"
  self.format = :json
  self.include_root_in_json = false
  self.element_name = "class"
  self.primary_key = :classId

  def find_or_create_resource_for_collection(name)
    case name
    when :mdAttributes
      self.class.const_get(:Cms).const_get(:AttrMd)
    else
      super
    end
  end
  
  def to_param
    className.to_s
  end
   
  # modify standard paths to not include format extension
  # the format is already defined in the header
  def self.new_element_path(prefix_options = {})
    drop_extension(super)
  end

  def self.element_path(id, prefix_options = {}, query_options = nil)
    drop_extension(super)
  end

  def self.collection_path(prefix_options = {}, query_options = nil)
    drop_extension(super)
  end

  private

  def self.drop_extension(path)
    path.gsub(/.#{self.format.extension}/, '')
  end
end

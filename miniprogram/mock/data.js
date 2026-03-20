const categoryList = ['专柜系', '被子', '枕头', '套件', '周边', '床盖', '凉席', '婚嫁']

const products = [
  {
    id: 'p1001',
    category: '套件',
    title: '60支长绒棉四件套',
    subtitle: '亲肤透气 · 轻奢卧室',
    soldCount: 128,
    image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Yu+Home+01',
    price: 399,
    originPrice: 699,
    skuList: [
      { size: '1.5m', material: '全棉', price: 369, originPrice: 639, stock: 52, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=1.5m+Cotton' },
      { size: '1.8m', material: '全棉', price: 399, originPrice: 699, stock: 40, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=1.8m+Cotton' },
      { size: '2.0m', material: '天丝', price: 469, originPrice: 799, stock: 21, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=2.0m+Tencel' }
    ]
  },
  {
    id: 'p1002',
    category: '被子',
    title: '抗菌羽丝绒冬被',
    subtitle: '保暖蓬松 · 秋冬推荐',
    soldCount: 86,
    image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Yu+Home+02',
    price: 269,
    originPrice: 469,
    skuList: [
      { size: '150x200', material: '磨毛', price: 249, originPrice: 429, stock: 34, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=150x200' },
      { size: '200x230', material: '磨毛', price: 269, originPrice: 469, stock: 27, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=200x230' }
    ]
  },
  {
    id: 'p1003',
    category: '床盖',
    title: 'A类全棉床笠三件套',
    subtitle: '柔软不易起球',
    soldCount: 234,
    image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Yu+Home+03',
    price: 199,
    originPrice: 329,
    skuList: [
      { size: '1.5m', material: '全棉', price: 189, originPrice: 319, stock: 65, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Bed+1.5m' },
      { size: '1.8m', material: '全棉', price: 199, originPrice: 329, stock: 46, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Bed+1.8m' }
    ]
  },
  {
    id: 'p1004',
    category: '枕头',
    title: '轻奢乳胶护颈枕',
    subtitle: '慢回弹 · 分区支撑',
    soldCount: 310,
    image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Yu+Home+04',
    price: 159,
    originPrice: 259,
    skuList: [
      { size: '标准款', material: '乳胶', price: 159, originPrice: 259, stock: 83, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=Std' },
      { size: '加高款', material: '乳胶', price: 179, originPrice: 289, stock: 57, image: 'https://dummyimage.com/400x400/f6f6f6/999999&text=High' }
    ]
  }
]

module.exports = {
  categoryList,
  products
}

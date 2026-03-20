Component({
  properties: {
    product: {
      type: Object,
      value: {}
    },
    layout: {
      type: String,
      value: 'grid'
    }
  },
  methods: {
    onTapCard() {
      this.triggerEvent('tap', { id: this.data.product.id })
    },
    onAddCart() {
      this.triggerEvent('addcart', { id: this.data.product.id })
    }
  }
})

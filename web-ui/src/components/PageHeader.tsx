import { Code2 } from 'lucide-react'
import './PageHeader.css'

function PageHeader() {
  return (
    <div className="page-header">
      <Code2 size={16} className="page-header__icon" />
      <span className="page-header__title">Python 3 Integration</span>
    </div>
  )
}

export default PageHeader

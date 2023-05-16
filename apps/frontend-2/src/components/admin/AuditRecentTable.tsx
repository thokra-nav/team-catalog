import React, {useEffect, useState} from 'react'
import {PageResponse, AuditItem, ObjectType} from '../../constants'
import {useNavigate} from 'react-router-dom'
import { getAudits } from '../../api/adminApi'
import { css } from '@emotion/css'
import { Label, Pagination, Table } from '@navikt/ds-react'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import { BasicSelect } from '../select/CustomSelectComponents'

dayjs.extend(relativeTime)

export const AuditRecentTable = (props: { show: boolean }) => {
  const [audits, setAudits] = useState<PageResponse<AuditItem>>({ content: [], numberOfElements: 0, pageNumber: 0, pages: 0, pageSize: 1, totalElements: 0 })
  const [page, setPage] = useState(1)
  const [limit, setLimit] = useState(20)
  const [table, setTable] = useState<ObjectType | undefined>(undefined)
  const navigate = useNavigate()

  useEffect(() => {
    ;(async () => {
      props.show && setAudits(await getAudits(page - 1, limit, table))
    })()
  }, [page, limit, props.show, table])

  const handlePageChange = (nextPage: number) => {
    if (nextPage < 1) {
      return
    }
    if (nextPage > audits.pages) {
      return
    }
    setPage(nextPage)
  }

  useEffect(() => {
    const nextPageNum = Math.ceil(audits.totalElements / limit)
    if (audits.totalElements && nextPageNum < page) {
      setPage(nextPageNum)
    }
  }, [limit, audits.totalElements])

  if (!props.show) {
    return null
  }

  return (
    <>
        <div className={css`display: flex; justify-content: space-between; margin-top: 2rem; margin-bottom: 1rem; align-items: center;`}>
            <Label>Siste endringer</Label>

            <div className={css`display: flex; gap: 1rem; min-width: 400px; align-items: center;`}>
                <Label>Tabellnavn: </Label>

                <BasicSelect
                   options={Object.keys(ObjectType).map((ot) => ({ id: ot, label: ot }))}
                   onChange={(p) => setTable(p?.id as ObjectType)}
                   className={css`min-width: 150px;`}
                />
            </div>
        </div>

        <Table>
            <Table.Header>
                <Table.Row>
                    <Table.HeaderCell scope="col">Tidspunkt</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Aksjon</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Id</Table.HeaderCell>
                    <Table.HeaderCell scope="col">Bruker</Table.HeaderCell>
                    <Table.HeaderCell scope="col">-</Table.HeaderCell>
                </Table.Row>
            </Table.Header>
            <Table.Body>
                {audits.content.map((audit, i) => {
                return (
                    <Table.Row key={i + audit.id}>
                        <Table.HeaderCell scope="row">{dayjs(audit.time).fromNow(true)} siden</Table.HeaderCell>
                        <Table.DataCell>{audit.table}</Table.DataCell>
                        <Table.DataCell>{audit.id}</Table.DataCell>
                        <Table.DataCell>{audit.user}</Table.DataCell>
                        <Table.HeaderCell scope="col"></Table.HeaderCell>
                    </Table.Row>
                );
                })}
            </Table.Body>
        </Table>

        <div className={css`display: flex; justify-content: flex-end; margin-top: 1rem;`}>
          <Pagination boundaryCount={1} count={limit} onPageChange={(x) => handlePageChange(x)} page={page} siblingCount={1} />
        </div>
    </>
  )
}